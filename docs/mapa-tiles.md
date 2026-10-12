# Erro 403 nos tiles do OpenStreetMap

## O sintoma

No lugar do mapa aparecem quadrados cinzas com a mensagem:

> **403 — Access blocked**
> App is not following the tile usage policy of OpenStreetMap's volunteer-run servers: osm.wiki/Blocked

Não é bug do Leaflet nem falha de rede: é o servidor de tiles recusando a requisição.

## As três causas, e qual delas atingia o MobiliTudo

A [política de uso dos tiles da OSMF](https://operations.osmfoundation.org/policies/tiles/)
exige que cada requisição seja atribuível a uma aplicação identificável. O projeto violava
dois pontos ao mesmo tempo e esbarrava num terceiro durante o desenvolvimento.

### 1. URL com subdomínios descontinuados

```js
// antes
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { attribution: '© OpenStreetMap' })
```

Os subdomínios `a.`, `b.` e `c.tile.openstreetmap.org` foram descontinuados. A política é
explícita: o endereço válido é `https://tile.openstreetmap.org/{z}/{x}/{y}.png`, e outros
hostnames "podem ser mais lentos ou removidos sem aviso".

### 2. Atribuição incompleta

A string `'© OpenStreetMap'` não atende ao requisito de atribuição: é preciso o crédito
visível com link para a licença.

### 3. Página aberta por `file://` — a causa imediata

Esta é a que derruba o mapa durante o desenvolvimento, e nenhuma mudança no JavaScript
resolve. Abrindo `index.html` com dois cliques, o navegador carrega a página pelo esquema
`file://` e **não envia cabeçalho `Referer` algum**. Os servidores do OSM bloqueiam
requisições anônimas por padrão, e mapas em arquivos locais são atingidos como efeito
colateral disso.

Nenhuma página consegue forjar o próprio `Referer`: o cabeçalho é controlado pelo
navegador. A única saída é servir os arquivos por HTTP.

## O que foi feito

| Correção | Onde |
|---|---|
| URL sem `{s}`, atribuição completa com link, `maxZoom: 19` | [`js/config.js`](../js/config.js) |
| `<meta name="referrer" content="strict-origin-when-cross-origin">` | [`index.html`](../index.html), [`config.html`](../config.html) |
| Detecção de `file://` antes da primeira requisição, com provedor alternativo e aviso explicando como servir por HTTP | [`js/mapa.js`](../js/mapa.js) |
| Troca automática de provedor após erros repetidos de tile | [`js/mapa.js`](../js/mapa.js) |
| Escolha manual do provedor | [`config.html`](../config.html) |
| Front servido por nginx, com `Referer` válido | [`docker-compose.yml`](../docker-compose.yml) |

Sobre a `<meta name="referrer">`: os navegadores atuais já usam
`strict-origin-when-cross-origin` por padrão, mas declarar o valor evita que uma política
mais restritiva — herdada de um cabeçalho do servidor ou de uma configuração de host —
remova o `Referer` sem que ninguém perceba.

## Como rodar sem tomar 403

Qualquer uma destas serve a pasta por HTTP:

```bash
npx serve .                 # Node
python -m http.server 8081  # Python
docker compose up --build   # projeto inteiro: banco, API e mapa
```

Depois abra `http://localhost:8081` — não `file:///.../index.html`.

## Se o bloqueio persistir

O bloqueio também pode vir do volume de requisições ou de um IP compartilhado já
penalizado. Nesse caso:

1. Em **Configurações → Provedor das imagens do mapa**, escolha `CARTO Voyager`.
2. Para uso contínuo ou publicação, contrate um provedor com chave própria
   (MapTiler, Thunderforest, Stadia Maps) e aponte o `url` em `js/config.js` para ele.

Os servidores do OpenStreetMap são mantidos por doação e trabalho voluntário. Um projeto
de extensão cabe folgadamente no uso aceitável; um mapa publicado com tráfego real deve
usar provedor próprio.
