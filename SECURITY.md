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
- se for um problema de dados pessoais (ver LGPD abaixo), quais dados ficam
  expostos e para quem.

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

- **Curadoria de obstáculos** (`POST /api/v1/obstaculos/{id}/validacao`) é o único
  endpoint autenticado hoje, por um segredo compartilhado
  (`MOBILITUDO_CURADOR_TOKEN`), não por conta de usuário individual — ver
  [`FiltroAutenticacaoCurador`](backend/src/main/java/br/com/mobilitudo/compartilhado/infraestrutura/seguranca/FiltroAutenticacaoCurador.java).
  Um token vazado permite validar obstáculos falsos (desviando rotas de verdade) ou
  resolver obstáculos reais (apagando-os do mapa). Se um token de produção vazar,
  é uma vulnerabilidade de severidade alta — reporte por aqui, e troque o valor de
  `MOBILITUDO_CURADOR_TOKEN` imediatamente.
- **Dados pessoais**: relatos de obstáculo aceitam um `reportadoPor` em texto
  livre, e tanto locais quanto obstáculos carregam coordenadas geográficas. Uma
  falha que exponha esse vínculo (quem reportou o quê e onde) além do já público
  pela própria API é tratada como incidente de dado pessoal, não só como bug.
- **Limite de relatos** (`FiltroLimiteDeObstaculos`) é defesa contra abuso de
  volume, não autenticação — encontrar uma forma de contorná-lo (por IP spoofing,
  por exemplo) é uma vulnerabilidade válida de se reportar, mas de severidade
  menor que as duas acima.

## O que não é escopo de segurança

Bug de UX, sugestão de feature, ou imprecisão no mapeamento de uma calçada são bem-vindos — mas como [issue pública normal](https://github.com/YasKarmem/MobiliTudo/issues), não como relato de segurança.
