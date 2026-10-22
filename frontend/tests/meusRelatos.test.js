import { beforeAll, beforeEach, describe, expect, test } from 'vitest';
import { carregarScript } from './auxiliares/carregarScript.js';

beforeAll(() => {
    carregarScript('js/meusRelatos.js');
});

beforeEach(() => {
    window.localStorage.clear();
});

const OBSTACULO_EXEMPLO = {
    id: 'abc-123',
    tipoDescricao: 'Degrau sem rampa',
    coords: [-23.99, -46.41],
    reportadoEm: '2026-10-12T10:00:00Z'
};

describe('adicionar', () => {
    test('sem id ou sem token, não registra nada', () => {
        window.MobiliTudoMeusRelatos.adicionar(null, 'token');
        window.MobiliTudoMeusRelatos.adicionar({}, 'token');
        window.MobiliTudoMeusRelatos.adicionar(OBSTACULO_EXEMPLO, null);

        expect(window.MobiliTudoMeusRelatos.listar()).toHaveLength(0);
    });

    test('registra o par obstáculo/token e os campos relevantes para exibição', () => {
        window.MobiliTudoMeusRelatos.adicionar(OBSTACULO_EXEMPLO, 'token-secreto');

        var lista = window.MobiliTudoMeusRelatos.listar();
        expect(lista).toHaveLength(1);
        expect(lista[0]).toMatchObject({
            id: 'abc-123',
            tokenExclusao: 'token-secreto',
            tipoDescricao: 'Degrau sem rampa'
        });
    });

    test('relatos mais recentes aparecem primeiro', () => {
        window.MobiliTudoMeusRelatos.adicionar({ ...OBSTACULO_EXEMPLO, id: 'primeiro' }, 'tok1');
        window.MobiliTudoMeusRelatos.adicionar({ ...OBSTACULO_EXEMPLO, id: 'segundo' }, 'tok2');

        var lista = window.MobiliTudoMeusRelatos.listar();
        expect(lista.map(function (r) { return r.id; })).toEqual(['segundo', 'primeiro']);
    });

    test('a lista não cresce sem limite', () => {
        for (var i = 0; i < 250; i++) {
            window.MobiliTudoMeusRelatos.adicionar({ ...OBSTACULO_EXEMPLO, id: 'relato-' + i }, 'token-' + i);
        }

        expect(window.MobiliTudoMeusRelatos.listar().length).toBeLessThanOrEqual(200);
    });
});

describe('remover', () => {
    test('remove só o item pedido, mantendo os outros', () => {
        window.MobiliTudoMeusRelatos.adicionar({ ...OBSTACULO_EXEMPLO, id: 'fica' }, 'tok1');
        window.MobiliTudoMeusRelatos.adicionar({ ...OBSTACULO_EXEMPLO, id: 'sai' }, 'tok2');

        window.MobiliTudoMeusRelatos.remover('sai');

        var lista = window.MobiliTudoMeusRelatos.listar();
        expect(lista.map(function (r) { return r.id; })).toEqual(['fica']);
    });

    test('remover um id inexistente não lança erro', () => {
        expect(() => window.MobiliTudoMeusRelatos.remover('nao-existe')).not.toThrow();
    });
});

describe('persistência', () => {
    test('fica em localStorage, sobrevivendo a uma "nova carga de página" (novo require do módulo)', () => {
        window.MobiliTudoMeusRelatos.adicionar(OBSTACULO_EXEMPLO, 'token-secreto');

        // Simula reabrir a página: localStorage persiste, só o módulo em memória é
        // recriado — exatamente o que acontece ao navegar entre perfil.html e index.html.
        carregarScript('js/meusRelatos.js');

        expect(window.MobiliTudoMeusRelatos.listar()).toHaveLength(1);
    });

    test('JSON corrompido em localStorage não quebra a leitura, só volta lista vazia', () => {
        window.localStorage.setItem('mobilitudo.meusRelatos', '{isto não é json válido');

        expect(window.MobiliTudoMeusRelatos.listar()).toEqual([]);
    });
});
