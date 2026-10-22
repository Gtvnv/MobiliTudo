# Contribuindo com o MobiliTudo

Obrigado por considerar contribuir. O MobiliTudo é um projeto social, sem fins
lucrativos e sem plano de cobrar por nenhuma funcionalidade — o valor dele está
inteiramente em quantas pessoas conseguem confiar nas rotas que ele sugere, e isso
depende de contribuição externa: mais quarteirões mapeados, mais obstáculos
conferidos, mais olhos revisando o algoritmo que decide por onde alguém consegue
passar.

Qualquer contribuição é bem-vinda: código, dados de mapeamento, correção de
documentação, relato de problema de acessibilidade na própria interface, tradução,
ou só uma pergunta aberta como issue.

## Licença

O projeto é licenciado sob **AGPL-3.0-or-later** (ver [`LICENSE`](LICENSE)). Ao abrir
um pull request, você concorda em licenciar sua contribuição nos mesmos termos — é o
que mantém o código e qualquer modificação, inclusive rodando atrás de uma API
privada, acessíveis de volta à comunidade.

## Código de conduta

Sem burocracia: trate as outras pessoas com respeito, assuma boa intenção em
discussões técnicas, e lembre que o público final deste projeto são pessoas com
deficiência tentando se locomover numa cidade — decisões de produto e de prioridade
devem sempre voltar para isso. Comportamento abusivo, assédio ou discriminação não
são tolerados e podem levar ao bloqueio de participação no repositório.

## Como rodar o projeto localmente

Veja o [README](README.md#como-subir) para o passo a passo completo. Resumindo:

```bash
docker compose up --build       # stack inteira: banco, API, mapa
```

Para trabalhar só no backend ou só no front, veja as seções "Backend em
desenvolvimento" e "Só o mapa, sem backend" do README.

## Fluxo de contribuição

1. **Abra uma issue antes de mudanças grandes.** Para correções pequenas (typo,
   bug óbvio, ajuste de doc) pode ir direto para o PR.
2. **Fork o repositório** e crie uma branch a partir de `main`:
   `git checkout -b feat/nome-descritivo-da-mudanca` (ou `fix/`, `docs/`, `refactor/`
   conforme o caso). Nunca commit direto em `main`.
3. **Escreva testes para o que mudar.** O domínio do backend é livre de framework de
   propósito — roda em milissegundos, sem banco — então não há desculpa para pular
   teste unitário ali. Mudança de comportamento visível no front ganha teste em
   `frontend/tests/`.
4. **Rode a suíte inteira antes de abrir o PR:**
   ```bash
   cd backend && mvn verify -DskipITs=false   # exige Docker
   cd frontend && npm test
   ```
5. **Mensagens de commit**: descreva o quê e, principalmente, o porquê — "corrige X"
   diz menos que "corrige X porque Y causava Z". Preferência por português, como o
   resto do projeto.
6. **Abra o PR** contra `main` do repositório upstream, descrevendo o que mudou e
   como foi verificado. O CI roda os testes de backend e frontend automaticamente.

## Convenções do código

O projeto é bilíngue de propósito: **identificadores em português** (classes,
métodos, variáveis, nomes de teste) em todo o código — backend e front —, porque o
domínio é municipal e os termos certos (rebaixamento de guia, piso tátil, calçada)
não têm tradução natural que valha a pena. Comentários em código, documentação e
mensagens de commit também em português.

### Backend (Java / Spring Boot)

Arquitetura hexagonal em três contextos delimitados (`acessibilidade`, `obstaculos`,
`rotas`), com o vocabulário comum em `compartilhado`. Detalhes em
[`docs/arquitetura.md`](docs/arquitetura.md). Duas regras que o projeto leva a
sério:

- **O pacote `dominio` não importa nada de Spring, JPA ou qualquer framework.** As
  regras de acessibilidade e o algoritmo de rota precisam continuar testáveis sem
  banco e sem contexto de aplicação.
- **Comentários explicam o porquê, não o quê.** Um comentário que só repete o nome
  do método não ajuda ninguém; um que explica por que uma heurística precisa de um
  fator mínimo para o A* continuar correto, sim.

### Front-end (HTML/CSS/JS, sem build)

`frontend/public/` é servido como está — sem bundler, sem transpilação. Cada módulo
em `js/` é uma IIFE que se registra em `window.MobiliTudoAlgumaCoisa`, para que
páginas diferentes só incluam o que precisam via `<script src="...">`. Novo módulo
segue o mesmo padrão; ver `js/texto.js` como exemplo mínimo.

## Mapeamento de dados

A malha viária hoje cobre só um recorte demonstrativo de Praia Grande. Ampliar essa
cobertura — novos trechos de calçada com largura, inclinação, piso e obstáculos
reais levantados em campo — é tão valioso quanto uma contribuição de código, e pode
ser feito direto pelas migrações Flyway em
`backend/src/main/resources/db/migration/` ou relatando obstáculos pela própria
interface.

## Dúvidas

Abra uma issue. Não há SLA de resposta — é um projeto mantido por voluntários —, mas
toda issue é lida.
