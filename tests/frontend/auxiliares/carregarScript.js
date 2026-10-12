// Carrega um dos scripts "clássicos" do projeto (js/*.js, sem export algum, escritos
// como `(function (global) { ... }(window))`) no ambiente de teste.
//
// Eles não são módulos ES — não têm import/export — e dependem de rodar em escopo
// global para que `window` resolva ao jsdom que o vitest já expõe. `eval` indireto
// ((0, eval)(codigo)) faz exatamente isso: roda o código no escopo global do realm, e
// não no escopo do módulo de teste que o chama. É o mesmo efeito de uma tag <script> comum
// no navegador, só que lendo o arquivo do disco em vez de buscá-lo por HTTP.
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const raizDoProjeto = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');

export function carregarScript(caminhoRelativoAJs) {
    const caminho = path.join(raizDoProjeto, caminhoRelativoAJs);
    const codigo = readFileSync(caminho, 'utf-8');
    // eslint-disable-next-line no-eval
    (0, eval)(codigo);
}
