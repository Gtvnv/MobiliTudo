import { beforeAll, describe, expect, test } from 'vitest';
import { carregarScript } from './auxiliares/carregarScript.js';

beforeAll(() => {
    carregarScript('js/texto.js');
});

describe('MobiliTudoTexto.escapar', () => {
    test('escapa os cinco caracteres perigosos de HTML', () => {
        expect(window.MobiliTudoTexto.escapar('<script>alert("x")</script>&\'oi\''))
            .toBe('&lt;script&gt;alert(&quot;x&quot;)&lt;/script&gt;&amp;&#39;oi&#39;');
    });

    test('texto sem caracteres especiais sai inalterado', () => {
        expect(window.MobiliTudoTexto.escapar('Palácio das Artes')).toBe('Palácio das Artes');
    });

    test('null e undefined viram string vazia, não "null"/"undefined"', () => {
        expect(window.MobiliTudoTexto.escapar(null)).toBe('');
        expect(window.MobiliTudoTexto.escapar(undefined)).toBe('');
    });

    test('números e outros tipos são convertidos para string antes de escapar', () => {
        expect(window.MobiliTudoTexto.escapar(42)).toBe('42');
    });

    test('uma tentativa de fuga de atributo onclick é neutralizada', () => {
        // É exatamente o padrão usado em curadoria.html: o id entra dentro de um
        // onclick="...('ID')". Se o id pudesse conter aspas simples cruas, um valor como
        // este encerraria a string do atributo e injetaria JavaScript arbitrário.
        var tentativaDeFuga = "x'); alert('pwned";

        var escapado = window.MobiliTudoTexto.escapar(tentativaDeFuga);

        expect(escapado).not.toContain("'");
        expect(escapado).toBe('x&#39;); alert(&#39;pwned');
    });
});
