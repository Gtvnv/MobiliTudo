# Política de Segurança

## Versões suportadas

O MobiliTudo ainda não tem versão estável (`0.x`, pré-1.0). Só a ponta de `main`
recebe correção de segurança — não há branch de manutenção de versão antiga.

## Como reportar uma vulnerabilidade

**Não abra uma issue pública para uma vulnerabilidade ainda não corrigida.** Um
relato público antes da correção dá a qualquer pessoa o roteiro para explorá-la
enquanto ainda está aberta.

Use o recurso de **relato privado de vulnerabilidade do GitHub**: na aba
**Security** do repositório, clique em **Report a vulnerability**. Isso abre uma
conversa privada com os mantenedores, sem expor o problema publicamente, e sem
exigir que você descubra um e-mail de contato.

Inclua, na medida do possível:

- o que o problema permite fazer (impacto), não só onde ele está;
- passos para reproduzir, ou uma prova de conceito;
- se for um problema de dados pessoais (ver [docs/privacidade.md](docs/privacidade.md)),
  quais dados ficam expostos e para quem.

## Expectativa de resposta

O projeto é mantido por voluntários, sem SLA formal. Na prática, um relato de
segurança é tratado com prioridade sobre qualquer outra pendência — é razoável
esperar um primeiro retorno em poucos dias, mas isso não é uma garantia contratual.

## Divulgação coordenada

Pedimos um prazo razoável para corrigir antes de qualquer divulgação pública do
problema. Depois da correção publicada, relatos relevantes podem ser creditados
(com seu consentimento) nas notas da correção.

## Áreas sensíveis deste projeto, especificamente

Vale saber, ao avaliar se algo é uma vulnerabilidade real aqui:

- **Curadoria de obstáculos** (`POST /api/v1/obstaculos/{id}/validacao`) exige
  conta individual de curador (e-mail + senha, ver
  [`curadores`](backend/src/main/java/br/com/mobilitudo/curadores/)), autenticada
  por token de sessão enviado como `Authorization: Bearer <token>`. A senha é
  hash PBKDF2-HMAC-SHA256 (210.000 iterações); o token de sessão é opaco e
  revogável (logout, desativação de conta), com só o hash persistido. Uma conta
  comprometida permite validar obstáculos falsos (desviando rotas de verdade) ou
  resolver obstáculos reais (apagando-os do mapa) — reporte por aqui, e desative
  a conta (`POST /curadores/{id}/desativar`) imediatamente.
- **Dados pessoais**: relatos de obstáculo aceitam um `reportadoPor` em texto
  livre, e tanto locais quanto obstáculos carregam coordenadas geográficas. Uma
  falha que exponha esse vínculo (quem reportou o quê e onde) além do já público
  pela própria API é tratada como incidente de dado pessoal, não só como bug. Ver
  [docs/privacidade.md](docs/privacidade.md) para o que é dado público de
  propósito (o acervo de obstáculos em si) versus o que não deveria vazar.
- **Token de exclusão de obstáculo** (`POST /obstaculos/{id}/anonimizar`, ver
  [`TokenDeExclusao`](backend/src/main/java/br/com/mobilitudo/obstaculos/dominio/servico/TokenDeExclusao.java))
  é a prova de posse que permite a quem relatou remover o próprio nome depois.
  Só o hash SHA-256 fica no banco; encontrar uma forma de recuperar o texto puro
  a partir do hash, ou de anonimizar um relato sem o token correto, é
  vulnerabilidade de severidade alta.
- **Limite de relatos e de login**
  ([`LimitadorDeTaxa`](backend/src/main/java/br/com/mobilitudo/compartilhado/infraestrutura/seguranca/LimitadorDeTaxa.java))
  é defesa contra abuso de volume e contra força bruta de senha, não o mecanismo
  de autenticação em si — encontrar uma forma de contorná-lo (por IP spoofing,
  por exemplo) é uma vulnerabilidade válida de se reportar, mas de severidade
  menor que as outras deste item.

## O que não é escopo de segurança

Bug de UX, sugestão de feature, ou imprecisão no mapeamento de uma calçada são bem-vindos — mas como [issue pública normal](https://github.com/YasKarmem/MobiliTudo/issues), não como relato de segurança.
