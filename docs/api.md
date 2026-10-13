# API MobiliTudo v1

Documentação interativa (Swagger UI) com a API no ar: <http://localhost:8080/docs>.

Todas as coordenadas são WGS 84 (EPSG:4326). Erros seguem
[RFC 7807](https://datatracker.ietf.org/doc/html/rfc7807) (`application/problem+json`).

Perfis aceitos em qualquer endpoint: `CADEIRA_DE_RODAS`, `MOBILIDADE_REDUZIDA`,
`DEFICIENCIA_VISUAL`, `PADRAO`. Valor ausente ou desconhecido cai em `PADRAO` — um perfil
escrito errado degrada a recomendação, não derruba a consulta.

### Paginação

Toda listagem (`GET /locais`, `GET /obstaculos`) aceita `pagina` (a partir de 0, padrão 0)
e `tamanho` (padrão 50, máximo 200 — acima disso responde 400). A resposta continua um
array simples, como antes; o cabeçalho `X-Tem-Proxima-Pagina` (`true`/`false`) diz se há
mais conteúdo além desta página, sem custar uma consulta `COUNT(*)` sobre a tabela
inteira.

Quando a listagem combina um filtro calculado em memória com a busca ao banco — categoria
depois de uma busca por proximidade, ou nível de acessibilidade por perfil — a página pode
voltar com menos itens que o `tamanho` pedido: o filtro atua sobre a página já paginada,
não sobre o acervo inteiro. Para os volumes atuais do projeto isso não chega a ser um
problema prático; ver a documentação de `ConsultarLocais` no código para o porquê.

### Autenticação da curadoria

Curadores têm conta própria (e-mail + senha), não um segredo compartilhado. Faça login em
`POST /curadores/login` para obter um token de sessão, e envie-o em todo endpoint que
exigir curadoria como `Authorization: Bearer <token>`. Sem o cabeçalho, com um token
inválido/expirado, ou com a conta desativada, responde `401` — sempre com a mesma
mensagem genérica, para não revelar qual dos casos é. Reportar um obstáculo
(`POST /obstaculos`) continua sem exigir nada: é a confirmação que precisa de confiança,
não o relato. Ver **Curadores** abaixo para o fluxo completo.

### Limite de relatos e de login

`POST /obstaculos` aceita no máximo `MOBILITUDO_LIMITE_OBSTACULOS_HORA` relatos por hora
por endereço IP (padrão 20). `POST /curadores/login` aceita no máximo
`MOBILITUDO_LIMITE_LOGIN_HORA` tentativas por hora por IP (padrão 10) — diferente de um
token de alta entropia, uma senha está sujeita a força bruta. Acima de qualquer um dos
dois limites, responde `429 Too Many Requests`. Não afeta leitura.

---

## Locais

### `GET /api/v1/locais`

| Parâmetro | Tipo | Descrição |
|---|---|---|
| `categoria` | texto | `shopping`, `praia`, `transporte`, `cultura`, `saude`, `educacao`, `servico-publico`, `outro` |
| `lat`, `lng` | número | busca por proximidade; exigem-se os dois |
| `raio` | número | raio em metros (padrão 2000) |
| `perfil` | texto | classifica cada local e, na busca por proximidade, omite os inacessíveis |
| `pagina`, `tamanho` | número | paginação — ver acima |

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

`status` (padrão `VALIDADO`), ou `lat` + `lng` + `raio` para busca por proximidade. Aceita
`pagina`/`tamanho`, como `/locais`.

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
Não exige autenticação; está sujeito ao limite de relatos por hora (ver acima).

A resposta traz um campo a mais que nenhuma outra resposta de obstáculo tem:

```json
{
  "id": "a1b2c3d4-...",
  "tipo": "DEGRAU",
  "status": "PENDENTE",
  "tokenExclusao": "xY9f2...q7A",
  "...": "..."
}
```

`tokenExclusao` é devolvido **uma única vez**, nesta resposta — não é recuperável
depois, nem pelo próprio relato (`GET /obstaculos` nunca inclui este campo). Guarde-o se
quiser remover seu nome do relato mais tarde (ver abaixo). Detalhes de por que isso
existe em [docs/privacidade.md](privacidade.md).

### `POST /api/v1/obstaculos/{id}/anonimizar`

Autoexclusão de dado pessoal (LGPD art. 18): remove o nome de quem relatou, sem apagar
o relato em si — a barreira continua no mapa, só deixa de estar associada a um nome.
**Não** exige o token de curadoria; exige só a posse do `tokenExclusao` devolvido na
criação.

```http
POST /api/v1/obstaculos/a1b2c3.../anonimizar
Content-Type: application/json

{ "tokenExclusao": "xY9f2...q7A" }
```

O token é de **uso único**: depois de uma anonimização bem-sucedida, o mesmo valor não
funciona mais. Token errado, já usado, ou relato sem token associado (por exemplo, dados
semeados direto no banco) respondem todos `403`, sem distinguir qual dos três casos é —
diferenciar daria a quem está tentando adivinhar um token informação sobre o que
quase acertou.

### `POST /api/v1/obstaculos/{id}/validacao`

Exige uma sessão de curador válida (ver **Autenticação da curadoria** acima e
**Curadores** abaixo).

```http
POST /api/v1/obstaculos/a1b2c3.../validacao
Authorization: Bearer <token de sessão>
Content-Type: application/json

{ "acao": "VALIDAR" }
```

Ações: `VALIDAR`, `REJEITAR`, `RESOLVER`. O ciclo de vida é
`PENDENTE → VALIDADO → RESOLVIDO` ou `PENDENTE → REJEITADO`; qualquer outra transição
responde `409`. Resolver um relato rejeitado seria afirmar que a cidade consertou algo que
nunca esteve lá. Sem uma sessão válida, `401`.

A resposta inclui `avaliadoPorEmail` — o e-mail de quem validou, rejeitou ou resolveu,
`null` para relatos ainda pendentes. É um retrato de quem avaliou no momento da
avaliação, não atualizado se a conta mudar de e-mail depois.

---

## Curadores

Conta de curador: e-mail, nome de exibição e senha (mínimo 8 caracteres). Não há papel de
administrador separado — qualquer curador ativo pode cadastrar ou desativar outro.

### `POST /api/v1/curadores/login`

```json
{ "email": "curadora@mobilitudo.org", "senha": "..." }
```

```json
{
  "token": "d6_xIkGGID-RO9GezSVKaIC53MgBtDeFBq33_iQhtQs",
  "expiraEm": "2026-11-12T03:13:19.013427936Z",
  "curador": { "id": "...", "email": "curadora@mobilitudo.org", "nomeDeExibicao": "...", "ativo": true, "criadoEm": "..." }
}
```

`token` é o segredo da sessão: existe em texto puro só nesta resposta (só o hash é
persistido) e deve ser enviado como `Authorization: Bearer <token>` em todo endpoint
autenticado. E-mail não cadastrado, senha errada ou conta desativada respondem `401` com a
mesma mensagem genérica. Acima do limite de tentativas por hora, `429`.

### `POST /api/v1/curadores/logout`

Exige `Authorization: Bearer <token>`. Encerra a sessão — o mesmo token deixa de
autenticar imediatamente. Idempotente: um token já inválido não gera erro. `204 No
Content`.

### `GET /api/v1/curadores/me`

Exige `Authorization: Bearer <token>`. Devolve os dados da conta autenticada.

### `GET /api/v1/curadores`

Exige uma sessão válida. Lista todas as contas de curador.

### `POST /api/v1/curadores`

Exige uma sessão válida.

```json
{ "email": "nova@mobilitudo.org", "nomeDeExibicao": "Nova Curadora", "senha": "senha-nova-123" }
```

`201` com a conta criada. `409` se o e-mail já estiver cadastrado; `400` para senha com
menos de 8 caracteres.

### `POST /api/v1/curadores/{id}/desativar`

Exige uma sessão válida. A conta desativada não autentica mais — nem com a senha certa,
nem por uma sessão já aberta antes da desativação (verificada a cada requisição).

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
