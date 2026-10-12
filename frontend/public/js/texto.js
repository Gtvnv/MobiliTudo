/**
 * Utilitários de texto compartilhados entre as páginas do mapa.
 *
 * Hoje só tem uma função, mas ela é sensível o bastante (é a barreira contra XSS ao
 * interpolar dado vindo da API em HTML) para merecer um módulo próprio em vez de ficar
 * copiada em cada página que precisa dela — como estava antes, duplicada
 * palavra-por-palavra em script.js e em curadoria.html.
 */
(function (global) {
    'use strict';

    /**
     * Escapa texto para interpolação segura em HTML, aspas inclusive.
     *
     * `textContent` sozinho cobre `&`, `<` e `>`, mas não as aspas — e o conteúdo aqui
     * entra dentro de atributos (como o `onclick` dos itens da curadoria), onde uma aspa
     * solta quebra a marcação e abre espaço para injeção.
     */
    function escapar(texto) {
        return String(texto === null || texto === undefined ? '' : texto)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    global.MobiliTudoTexto = {
        escapar: escapar
    };
}(window));
