# API MobiliTudo v1

Documentação interativa (Swagger UI) com a API no ar: <http://localhost:8080/docs>.

Todas as coordenadas são WGS 84 (EPSG:4326). Erros seguem
[RFC 7807](https://datatracker.ietf.org/doc/html/rfc7807) (`application/problem+json`).

Perfis aceitos em qualquer endpoint: `CADEIRA_DE_RODAS`, `MOBILIDADE_REDUZIDA`,
`DEFICIENCIA_VISUAL`, `PADRAO`. Valor ausente ou desconhecido cai em `PADRAO` — um perfil
escrito errado degrada a recomendação, não derruba a consulta.

---

## Locais

### `GET /api/v1/locais`

| Parâmetro | Tipo | Descrição |
|---|---|---|
| `categoria` | texto | `shopping`, `praia`, `transporte`, `cultura`, `saude`, `educacao`, `servico-publico`, `outro` |
| `lat`, `lng` | número | busca por proximidade; exigem-se os dois |
| `raio` | número | raio em metros (padrão 2000) |
| `perfil` | texto | classifica cada local e, na busca por proximidade, omite os inacessíveis |

```json
[
  {
    "id": "a1000000-0000-4000-8000-000000000003",
    "nome": "Palacio das Artes",
    "categoria": "cultura",
    "coords": [-23.992, -46.411],
    "endereco": "Av. Pres. Costa e Silva, 1600 - Boqueirao, Praia Grande - SP",
    "info": "Rampas de acesso, elevadores modernos e assentos reservados para PCD.",
    "recursos": ["Atendimento Prioritário", "Elevador", "Placas Braille", "Rampas"],
    "nivelAcessibilidade": "COM_RESSALVAS",
    "nivelDescricao": "Acessivel com ressalvas"
  }
]
```

`coords` e `recursos` mantêm o formato do antigo `locais.json`, de propósito: o mapa
consome a API sem reescrever a renderização da ficha do local.

### `GET /api/v1/locais/{id}` · `POST /api/v1/locais`

```json
{
  "nome": "Praça 19 de Janeiro",
  "categoria": "cultura",
  "coordenada": { "lat": -23.9931, "lng": -46.4102 },
  "endereco": "Boqueirão, Praia Grande - SP",
  "descricao": "Piso nivelado, bancos com espaço para cadeira de rodas.",
  "recursos": ["Rampas", "Piso Tátil"]
}
```

Rótulos de recurso não reconhecidos são ignorados em silêncio. Recusar o cadastro inteiro
por causa de uma grafia diferente desperdiçaria a contribuição de quem foi até o local
levantar a informação.

---

## Rotas

### `POST /api/v1/rotas/acessivel`

```json
{
  "origem":  { "lat": -23.9920, "lng": -46.4110 },
  "destino": { "lat": -23.9930, "lng": -46.4070 },
  "perfil": "CADEIRA_DE_RODAS"
}
```

```json
{
  "perfil": "CADEIRA_DE_RODAS",
  "perfilDescricao": "Cadeira de rodas",
  "distanciaMetros": 645.38,
  "custoPonderado": 712.04,
  "duracaoMinutos": 13.45,
  "nivelAcessibilidade": "PLENO",
  "nivelDescricao": "Acessivel",
  "passos": [
    {
      "instrucao": "Siga por Av. Pres. Costa e Silva por 104 m.",
      "via": "Av. Pres. Costa e Silva",
      "segmentoId": "av-1-2",
      "distanciaMetros": 103.9,
      "nivelAcessibilidade": "PLENO",
      "ressalvas": []
    }
  ],
  "avisos": [],
  "geometria": [[-23.992, -46.411], [-23.9922, -46.41]]
}
```

`geometria` já vem no formato que o Leaflet consome em `L.polyline`: pares
`[latitude, longitude]`.

`distanciaMetros` e `custoPonderado` quase nunca coincidem, e essa diferença é o ponto do
projeto — ela mede o quanto o trajeto acessível é mais penoso que a mesma distância em
calçada ideal.

#### Quando não há rota: `404` com motivo

```json
{
  "title": "Rota não encontrada",
  "status": 404,
  "detail": "Existe ligacao entre os pontos, mas nenhuma transponivel para o perfil Cadeira de rodas",
  "motivo": "SEM_CAMINHO_ACESSIVEL"
}
```

| `motivo` | Significado |
|---|---|
| `ORIGEM_FORA_DA_AREA_MAPEADA` | não há malha mapeada perto do ponto de partida |
| `DESTINO_FORA_DA_AREA_MAPEADA` | idem, para o destino |
| `SEM_CAMINHO_ACESSIVEL` | existe ligação física, mas nenhuma transponível pelo perfil |

A distinção importa: os dois primeiros são lacuna de mapeamento; o terceiro é uma barreira
urbana real isolando um destino — exatamente o dado que o projeto existe para expor.

---

## Obstáculos

### `GET /api/v1/obstaculos`

`status` (padrão `VALIDADO`), ou `lat` + `lng` + `raio` para busca por proximidade.

### `POST /api/v1/obstaculos`

```json
{
  "tipo": "DEGRAU",
  "severidade": "MODERADA",
  "coordenada": { "lat": -23.9925, "lng": -46.4085 },
  "descricao": "Degrau de 15 cm na entrada, sem rampa alternativa.",
  "segmentoViaId": "av-3-4",
  "reportadoPor": "yasmim"
}
```

O relato nasce `PENDENTE` e **não** influencia rota alguma. Só passa a desviar trajetos
depois de validado — um relato não conferido não pode bloquear o caminho de outra pessoa.

### `POST /api/v1/obstaculos/{id}/validacao`

```json
{ "acao": "VALIDAR" }
```

Ações: `VALIDAR`, `REJEITAR`, `RESOLVER`. O ciclo de vida é
`PENDENTE → VALIDADO → RESOLVIDO` ou `PENDENTE → REJEITADO`; qualquer outra transição
responde `409`. Resolver um relato rejeitado seria afirmar que a cidade consertou algo que
nunca esteve lá.

---

## Metadados

### `GET /api/v1/metadados`

Perfis, categorias, recursos, tipos de obstáculo e severidades, com rótulos e ícones.
Serve para montar os seletores do mapa a partir do domínio, em vez de listas fixas
duplicadas no JavaScript.

---

## Operação

| Endpoint | Para quê |
|---|---|
| `GET /actuator/health` | prontidão da API e do banco |
| `GET /docs` | Swagger UI |
| `GET /v3/api-docs` | especificação OpenAPI em JSON |
