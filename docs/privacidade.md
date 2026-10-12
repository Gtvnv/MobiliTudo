# Privacidade e dados pessoais

Este documento descreve, em termos concretos, quais dados o MobiliTudo coleta, por quê,
onde ficam, e como exercer os direitos prescritos pela Lei Geral de Proteção de Dados
(LGPD, Lei nº 13.709/2018) sobre eles. Não é um texto genérico copiado de modelo — cada
seção corresponde a um comportamento real do código, referenciado onde for útil conferir.

## Quais dados o sistema coleta, e por quê

### Ao reportar um obstáculo (base legal: legítimo interesse coletivo de acessibilidade)

| Dado | Obrigatório? | Para quê |
|---|---|---|
| Coordenada geográfica | Sim | É o próprio dado de acessibilidade — onde fica a barreira |
| Tipo, severidade, descrição | Sim/opcional | Descreve a barreira em si, não a pessoa |
| Nome de quem relata (`reportadoPor`) | Não | Atribuição opcional do relato; cai em "anônimo" se vazio |

O nome, quando preenchido, é o único dado pessoal de fato neste fluxo. Ele é **público**:
qualquer pessoa que consultar a API (`GET /api/v1/obstaculos`) vê quem relatou o quê e
onde — não há controle de acesso sobre essa informação hoje, porque o acervo de
obstáculos é, por natureza, dado aberto de interesse coletivo.

### Preferências salvas só no seu navegador (nunca enviadas ao servidor)

`perfil.html` grava em `localStorage`, só localmente: nome de exibição
(`nomeUsuario`), perfil de mobilidade (`tipoMobilidade`), e preferências de
acessibilidade da própria interface (alto contraste, fonte legível, leitor automático).
**Nada disso trafega pela rede** até que você use a função de relatar um obstáculo — aí
sim, o `nomeUsuario` salvo é lido e enviado como `reportadoPor` (ver abaixo).

### O vínculo entre o nome do perfil e o relato

Esta é a parte que merece atenção: o nome digitado em `perfil.html` para um simples
"Olá, fulano!" na barra lateral é o mesmo que acompanha, silenciosamente, todo relato de
obstáculo enviado a partir daquele navegador. Isso é deliberado — poupa de digitar o
nome de novo a cada relato —, mas significa que preencher o perfil tem uma consequência
que não é óbvia de cara: seu nome passa a ficar associado publicamente a cada
coordenada geográfica que você reportar.

## Onde os dados ficam

- **Banco de dados** (PostgreSQL/PostGIS): coordenadas, descrições e o nome de quem
  relatou, enquanto o relato não for anonimizado (ver abaixo).
- **Navegador de quem usa** (`localStorage`): preferências de perfil e, para quem
  relata um obstáculo, a lista "Meus relatos" — o par (id do obstáculo, token de
  exclusão) que permite remover o próprio nome depois. Essa lista **nunca é enviada ao
  servidor**; existe só para que o navegador "lembre" por você.
- **Logs de servidor**: como qualquer API web, requisições HTTP passam por logs de
  infraestrutura (endereço IP, timestamp) conforme a hospedagem usada em cada ambiente —
  isso está fora do controle da aplicação em si.

## Seus direitos, e como exercê-los

### Remover seu nome de um relato (LGPD art. 18, incisos IV e VI)

Isto é **self-service**, sem precisar pedir para ninguém:

1. Ao reportar um obstáculo, a API devolve um **token de exclusão** uma única vez — ver
   `tokenExclusao` na resposta de `POST /api/v1/obstaculos`
   ([docs/api.md](api.md)). O navegador que fez o relato guarda esse token
   automaticamente (em `localStorage`, nunca enviado ao servidor).
2. Em `perfil.html`, a seção **"Meus relatos"** lista os obstáculos relatados por aquele
   navegador, cada um com um botão **"Remover meu nome"**.
3. O servidor confere o token contra um hash SHA-256 salvo no banco — nunca o texto
   puro — e, se conferir, troca `reportadoPor` por `"anônimo"`. **O relato em si não é
   apagado**: a informação de acessibilidade (há um degrau ali, falta rampa aqui)
   continua disponível para quem precisar dela. É o nome que some, não o dado de
   interesse coletivo.
4. O token é de **uso único**: depois de usado, deixa de funcionar — mesmo que alguém
   mais tarde descubra o valor antigo (por exemplo, no histórico do navegador), ele não
   serve mais para nada.

Perdeu o acesso ao navegador que fez o relato, ou o relato é anterior a esta
funcionalidade (sem token associado)? Abra uma
[issue](https://github.com/YasKarmem/MobiliTudo/issues) pedindo a remoção manual,
identificando o relato (id, coordenada aproximada, data). Como o projeto não tem
sistema de contas, não há como verificar automaticamente que quem pede é quem relatou —
esse pedido passa por avaliação humana antes de ser atendido.

### Acesso e correção

Todo relato de obstáculo é consultável pela própria API (`GET /api/v1/obstaculos`), sem
necessidade de pedido formal — é dado público de propósito. Para corrigir uma descrição
ou severidade incorreta, use o mesmo canal de issue acima, ou — se você tiver acesso de
curadoria — edite diretamente.

### Portabilidade e eliminação completa de um relato

A anonimização (acima) cobre o caso mais comum. Pedido de remoção completa de um relato
(não só do nome) também passa pela issue, e é avaliado caso a caso: como a informação de
acessibilidade em si beneficia terceiros, a remoção total só se justifica quando o
próprio conteúdo do relato (não o nome) também precisa sumir — por exemplo, um relato
enviado por engano com coordenada errada.

## O que o sistema não faz

- Não usa cookies de rastreamento nem analytics de terceiros.
- Não compartilha dado nenhum com terceiros além do que já é público pela própria API.
- Não tem sistema de conta de usuário — não há senha, e-mail nem CPF coletado em lugar
  algum do fluxo de uso comum do mapa.
- Não verifica idade de quem usa; o serviço não é direcionado especificamente a
  crianças, mas também não tem controle de acesso por faixa etária.

## Medidas de segurança relacionadas a este documento

- Token de exclusão nunca persistido em texto puro — só o hash SHA-256
  ([`TokenDeExclusao`](../backend/src/main/java/br/com/mobilitudo/obstaculos/dominio/servico/TokenDeExclusao.java)).
- Comparação de token em tempo constante, para não vazar por *timing* quanto de um
  token um palpite acertou.
- O próprio código é auditável por qualquer pessoa — o projeto é
  [licenciado sob AGPL-3.0](../LICENSE) exatamente para isso.

Encontrou um problema de segurança relacionado a dado pessoal (não uma dúvida de
privacidade comum)? Veja [SECURITY.md](../SECURITY.md) para o canal de relato
responsável.
