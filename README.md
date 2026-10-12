# MobiliTudo

Mapeamento colaborativo de acessibilidade urbana em Praia Grande (SP), com cálculo de
rotas que contornam barreiras segundo o perfil de mobilidade de quem caminha.

Projeto de extensão — Engenharia de Software, Faculdade Descomplica.
Alinhado aos ODS **9** (Indústria, Inovação e Infraestrutura) e **11** (Cidades e
Comunidades Sustentáveis).

---

## A ideia

Mapas convencionais respondem "qual é o caminho mais curto". Para quem usa cadeira de
rodas, essa é a pergunta errada: o caminho mais curto pode ter um degrau, uma calçada de
80 cm ou uma travessia sem guia rebaixada — e qualquer um dos três o torna inútil.

O MobiliTudo responde outra pergunta: **qual é o caminho que eu consigo percorrer**. A
rota devolvida costuma ser mais longa em metros e mais curta em barreiras.

---

## Como subir

### Tudo de uma vez (Docker)

```bash
docker compose up --build
```

| | |
|---|---|
| Mapa | <http://localhost:8081> |
| API | <http://localhost:8080> |
| Documentação da API | <http://localhost:8080/docs> |

O banco já sobe com o esquema espacial e um recorte demonstrativo da malha viária entre o
Palácio das Artes e o Litoral Plaza — com uma obra no caminho curto, para que a diferença
entre a rota mais curta e a rota acessível apareça logo na primeira consulta.

### Só o mapa, sem backend

```bash
npx serve .                 # ou: python -m http.server 8081
```

O mapa funciona com o acervo local (`locais.json`). Rotas e relatos de barreira ficam
indisponíveis e dizem isso.

> **Não abra o `index.html` com dois cliques.** Páginas carregadas por `file://` não
> enviam cabeçalho `Referer`, e os servidores do OpenStreetMap recusam essas requisições
> com **403**. O mapa detecta a situação, troca de provedor e avisa — mas o jeito certo é
> servir por HTTP. Ver [docs/mapa-tiles.md](docs/mapa-tiles.md).

### Backend em desenvolvimento

```bash
docker compose up banco -d
cd backend
mvn spring-boot:run
```

| Variável | Padrão |
|---|---|
| `MOBILITUDO_DB_URL` | `jdbc:postgresql://localhost:5432/mobilitudo` |
| `MOBILITUDO_DB_USER` / `MOBILITUDO_DB_PASSWORD` | `mobilitudo` |
| `MOBILITUDO_PORT` | `8080` |
| `MOBILITUDO_CORS_ORIGENS` | `http://localhost:*,http://127.0.0.1:*` |

---

## Estrutura

```
index.html  config.html  perfil.html  sobre.html   mapa (Leaflet, sem build)
style.css   script.js    locais.json
js/
  config.js        endereço da API e provedores de tiles
  mapa.js          camada base com tolerância ao 403 do OSM
  api.js           cliente REST, com degradação para o acervo local
  rotas.js         painel de rota acessível
  obstaculos.js    relato colaborativo de barreiras
backend/           API Spring Boot + PostgreSQL/PostGIS (arquitetura hexagonal)
docs/              arquitetura, API e o diagnóstico do erro 403
docker-compose.yml
```

---

## O que o cálculo de rota leva em conta

Perfis: **cadeira de rodas**, **mobilidade reduzida**, **deficiência visual** e **sem
restrições**. Cada um define o que é intransponível e o que apenas encarece o trajeto —
parâmetros baseados na ABNT NBR 9050:2020 (faixa livre de 1,20 m, rampa de até 8,33%).

| Atributo do trecho | Efeito |
|---|---|
| Degrau, escada, ausência de calçada | barreira absoluta para quem anda sobre rodas |
| Faixa livre abaixo do mínimo do perfil | barreira |
| Inclinação acima do máximo do perfil | barreira |
| Superfície não firme (areia, terra, grama) | barreira para quem exige firmeza |
| Travessia sem rebaixamento de guia | barreira para cadeira de rodas, custo para os demais |
| Inclinação excedente, estreitamento, obstáculos leves, iluminação | aumentam o custo |
| Piso tátil | reduz o custo para quem tem deficiência visual |

Obstáculos reportados pela comunidade só influenciam rotas **depois de validados**: um
relato não conferido não pode bloquear o caminho de outra pessoa.

Detalhes do algoritmo em [docs/arquitetura.md](docs/arquitetura.md).

---

## Testes

```bash
cd backend
mvn test                          # 47 testes de domínio, sem banco, segundos
mvn verify -DskipITs=false        # + 6 testes de integração contra PostGIS real (exige Docker)
```

O domínio é livre de framework, então as regras de acessibilidade e o algoritmo de rota
rodam em teste unitário comum. Os testes de integração cobrem o que o unitário não
alcança: migrações Flyway, mapeamento das colunas `geometry`, consultas espaciais e a
rota ponta a ponta pela API.

---

## Documentação

- [Arquitetura](docs/arquitetura.md) — contextos delimitados, algoritmo de rota, persistência espacial
- [API v1](docs/api.md) — endpoints, exemplos e códigos de erro
- [Erro 403 nos tiles do OpenStreetMap](docs/mapa-tiles.md) — diagnóstico e correção

---

Desenvolvido por Yasmim Carmem Mariano Rocha · Engenharia de Software · 2026
