# Arquitetura do MobiliTudo

## Visão geral

```
┌──────────────────────────────────────────────┐
│  Mapa (HTML + Leaflet, sem build)            │
│  index.html · js/config, mapa, api,          │
│  rotas, obstaculos · script.js               │
└───────────────┬──────────────────────────────┘
                │ REST /api/v1
┌───────────────▼──────────────────────────────┐
│  API Spring Boot — arquitetura hexagonal     │
│                                              │
│   acessibilidade │ obstáculos │ rotas        │
│   ──────────────────────────────────────     │
│   infraestrutura  (web, persistência)        │
│   aplicação       (casos de uso)             │
│   domínio         (regras, sem framework)    │
└───────────────┬──────────────────────────────┘
                │ JDBC + Hibernate Spatial
┌───────────────▼──────────────────────────────┐
│  PostgreSQL 16 + PostGIS 3.4                 │
│  geometry(Point/LineString, 4326) · GiST     │
└──────────────────────────────────────────────┘
```

## Por que hexagonal

O valor do projeto está nas regras de acessibilidade, não no encanamento. Mantendo o
domínio livre de Spring e de JPA, a parte que importa roda em teste unitário comum: o
cálculo de rota tem 9 testes que sobem em frações de segundo, sem banco e sem contexto de
aplicação. O preço é uma classe de configuração que registra os serviços de domínio como
beans ([`ConfiguracaoDominio`](../backend/src/main/java/br/com/mobilitudo/compartilhado/infraestrutura/configuracao/ConfiguracaoDominio.java)).

### Três contextos delimitados

| Contexto | Responsabilidade | Agregado |
|---|---|---|
| `acessibilidade` | acervo de locais mapeados e seus recursos | `Local` |
| `obstaculos` | relato colaborativo e curadoria de barreiras | `Obstaculo` |
| `rotas` | malha viária e cálculo do trajeto | `SegmentoVia`, `GrafoVias` |

O que é vocabulário de todos fica em `compartilhado.dominio`: `Coordenada`,
`PerfilMobilidade`, `NivelAcessibilidade`, `TipoObstaculo`, `Severidade`, `AreaRetangular`.

### A fronteira entre obstáculos e rotas

O cálculo de rota precisa saber o que atrapalha cada trecho, mas não deve ler a tabela de
outro contexto. Entre os dois existe a porta
[`FonteDeObstaculos`](../backend/src/main/java/br/com/mobilitudo/rotas/dominio/porta/FonteDeObstaculos.java),
implementada pelo contexto de obstáculos
([`FonteDeObstaculosValidados`](../backend/src/main/java/br/com/mobilitudo/obstaculos/infraestrutura/adaptador/FonteDeObstaculosValidados.java)).

A travessia dessa fronteira é intencionalmente lossy: o roteamento recebe apenas
`ObstaculoNoTrecho(tipo, severidade)`. Quem reportou, quando e com qual status não é
assunto de quem traça a rota. E é o adaptador — não o domínio de rotas — que decide que
só obstáculos **validados** contam.

## O cálculo de rota

### Por que não é o caminho mais curto

A rota mais curta é frequentemente a menos acessível. O algoritmo é A* sobre a malha, mas
a função de custo não é a distância: é a distância ponderada pelo esforço real de vencer
cada trecho, dado o perfil de quem pediu.

Duas decisões distintas, em
[`AvaliadorAcessibilidade`](../backend/src/main/java/br/com/mobilitudo/rotas/dominio/servico/AvaliadorAcessibilidade.java):

**É transponível?** Se não, o trecho some do grafo. Encarecê-lo não bastaria — um caminho
caro ainda seria sugerido na falta de alternativa, e mandar uma cadeira de rodas por uma
escada é pior do que admitir que não há rota. Bloqueiam:

- obstáculo de severidade igual ou acima da que o perfil tolera;
- degrau, escada, ausência de calçada ou de rebaixamento de guia, para quem anda sobre rodas;
- faixa livre abaixo do mínimo do perfil (1,20 m para cadeira de rodas, NBR 9050);
- inclinação acima do máximo (8,33% para cadeira de rodas);
- superfície não firme, para quem exige firmeza.

**Quanto custa?** Um multiplicador sobre o comprimento. 1,0 é calçada ideal; 2,0 significa
que 100 m ali custam o mesmo que 200 m de calçada plana e larga. Entram superfície,
inclinação excedente, estreitamento, obstáculos não bloqueantes, travessia de rua e
iluminação. Piso tátil aplica desconto para quem tem deficiência visual.

### A heurística e o desconto de piso tátil

A* só garante o caminho ótimo com heurística admissível — ela nunca pode superestimar o
custo restante. A heurística natural é a distância em linha reta até o destino, o que
funciona enquanto todo trecho custa ao menos o próprio comprimento.

O desconto de piso tátil quebra isso: com multiplicador 0,85, um trecho pode custar menos
que o comprimento, e a linha reta passa a ser otimista demais. A correção é multiplicar a
heurística pelo menor fator possível do perfil
([`PerfilMobilidade.fatorMinimoDeCusto()`](../backend/src/main/java/br/com/mobilitudo/compartilhado/dominio/PerfilMobilidade.java)).
`AvaliadorAcessibilidadeTest` guarda essa invariante para todos os perfis.

A heurística também é **consistente**: como o comprimento de um trecho nunca é menor que a
distância geodésica entre seus extremos, vale `h(n) ≤ c(n,m) + h(m)`. É isso que autoriza
o algoritmo a fechar um nó na primeira vez que o retira da fila.

### O grafo é dirigido

Cada trecho bidirecional vira duas arestas, e a de volta carrega a inclinação com o sinal
trocado — descer uma ladeira não é o mesmo que subi-la. As duas arestas mantêm o mesmo id:
é a mesma calçada física, e é esse id que o usuário vê ao reportar uma barreira ali.

## Persistência espacial

O domínio não conhece JTS. A conversão entre `Coordenada` e `geometry(...,4326)` acontece
só em [`Geometrias`](../backend/src/main/java/br/com/mobilitudo/compartilhado/infraestrutura/persistencia/Geometrias.java).
Atenção à ordem dos eixos: no PostGIS, X é longitude e Y é latitude — invertida em relação
a como se fala uma coordenada.

Duas consultas nativas carregam o trabalho espacial:

- **proximidade**: `ST_DWithin(coluna::geography, ponto::geography, raio)` — o cast para
  `geography` faz o raio valer em metros reais, e o índice GiST atende o filtro;
- **recorte da malha**: `tracado && ST_MakeEnvelope(...)` — interseção de bounding boxes,
  também via índice. O filtro é por interseção, não por continência, para não perder
  trechos que apenas atravessam a área.

O recorte é dilatado por 600 m além do retângulo origem/destino, porque o desvio acessível
quase sempre sai da reta entre os dois pontos.

O esquema pertence ao Flyway; o Hibernate roda com `ddl-auto: validate`. Deixá-lo gerar
DDL faria as colunas `geometry` nascerem sem SRID e sem índice GiST.

## Testes

| Camada | O que cobre | Como rodar |
|---|---|---|
| Domínio (47 testes) | regras de acessibilidade, A*, ciclo de vida do obstáculo, geodésia | `mvn test` |
| Integração (6 testes) | migrações Flyway, mapeamento `geometry`, consultas espaciais, rota ponta a ponta | `mvn verify -DskipITs=false` (exige Docker) |

Os testes de integração ficam fora do build padrão porque dependem de Docker. Eles são o
único lugar que verifica o que um teste unitário não alcança — e foram eles que pegaram,
por exemplo, que o A* encontra um desvio mais barato do que o previsto na primeira
estimativa feita no papel.
