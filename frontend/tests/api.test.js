import { afterEach, beforeAll, beforeEach, describe, expect, test, vi } from 'vitest';
import { carregarScript } from './auxiliares/carregarScript.js';

beforeAll(() => {
    carregarScript('js/texto.js');
    carregarScript('js/config.js');
    carregarScript('js/api.js');
});

beforeEach(() => {
    window.localStorage.clear();
    window.sessionStorage.clear();
    vi.stubGlobal('location', { hostname: 'localhost', protocol: 'http:' });
    // Sem isto, baseDaApi() cairia no padrão de localhost de qualquer forma — fixado
    // explicitamente para o teste não depender desse detalhe de config.js.
    window.MobiliTudoConfig.definirBaseDaApi('http://localhost:8080');
});

afterEach(() => {
    vi.unstubAllGlobals();
});

function respostaFake(status, corpo) {
    return {
        ok: status >= 200 && status < 300,
        status: status,
        json: function () {
            return Promise.resolve(corpo);
        }
    };
}

describe('configurada', () => {
    test('reflete se a API tem endereço configurado', () => {
        expect(window.MobiliTudoApi.configurada()).toBe(true);

        window.MobiliTudoConfig.definirBaseDaApi('');
        vi.stubGlobal('location', { hostname: 'meusite.com.br', protocol: 'http:' });
        expect(window.MobiliTudoApi.configurada()).toBe(false);
    });
});

describe('listarLocais', () => {
    test('usa a API quando ela responde', async () => {
        var fetchFalso = vi.fn().mockResolvedValue(respostaFake(200, [{ nome: 'Da API' }]));
        vi.stubGlobal('fetch', fetchFalso);

        var locais = await window.MobiliTudoApi.listarLocais('PADRAO');

        expect(locais).toEqual([{ nome: 'Da API' }]);
        expect(fetchFalso).toHaveBeenCalledWith('http://localhost:8080/api/v1/locais', expect.anything());
        expect(window.MobiliTudoApi.online()).toBe(true);
    });

    test('perfil não-padrão entra na query string', async () => {
        var fetchFalso = vi.fn().mockResolvedValue(respostaFake(200, []));
        vi.stubGlobal('fetch', fetchFalso);

        await window.MobiliTudoApi.listarLocais('CADEIRA_DE_RODAS');

        expect(fetchFalso).toHaveBeenCalledWith(
            'http://localhost:8080/api/v1/locais?perfil=CADEIRA_DE_RODAS', expect.anything());
    });

    test('perfil padrão não polui a query string', async () => {
        var fetchFalso = vi.fn().mockResolvedValue(respostaFake(200, []));
        vi.stubGlobal('fetch', fetchFalso);

        await window.MobiliTudoApi.listarLocais('PADRAO');

        expect(fetchFalso).toHaveBeenCalledWith('http://localhost:8080/api/v1/locais', expect.anything());
    });

    test('cai para o acervo local quando a API falha, e marca offline', async () => {
        var fetchFalso = vi.fn()
            .mockRejectedValueOnce(new TypeError('Failed to fetch'))
            .mockResolvedValueOnce(respostaFake(200, [
                { nome: 'Local Offline', categoria: 'cultura', coords: [1, 2], info: 'x', imagem: null, recursos: ['Rampas'] }
            ]));
        vi.stubGlobal('fetch', fetchFalso);

        var locais = await window.MobiliTudoApi.listarLocais('PADRAO');

        expect(locais).toHaveLength(1);
        expect(locais[0].nome).toBe('Local Offline');
        // Normalizado para o mesmo formato que a API devolveria.
        expect(locais[0].id).toBeNull();
        expect(locais[0].nivelAcessibilidade).toBeNull();
        expect(window.MobiliTudoApi.online()).toBe(false);
        expect(fetchFalso).toHaveBeenLastCalledWith('locais.json');
    });
});

describe('estado online/offline', () => {
    test('uma falha de transporte marca offline', async () => {
        vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));

        await window.MobiliTudoApi.listarLocais('PADRAO').catch(function () {});

        expect(window.MobiliTudoApi.online()).toBe(false);
    });

    test('uma resposta HTTP de erro (4xx) não derruba o estado online', async () => {
        // Primeiro estabelece o estado online com uma chamada bem-sucedida; só então o
        // 4xx entra em cena. É a transição que importa: já estando online, uma resposta
        // de erro — a API respondeu, só não gostou do pedido — não deve apagar isso.
        // Diferente de estar fora do ar, não deve acender o aviso de "modo offline".
        var fetchFalso = vi.fn()
            .mockResolvedValueOnce(respostaFake(200, []))
            .mockResolvedValueOnce(respostaFake(400, { detail: 'Requisição inválida' }));
        vi.stubGlobal('fetch', fetchFalso);

        await window.MobiliTudoApi.listarLocais('PADRAO');
        expect(window.MobiliTudoApi.online()).toBe(true);

        await window.MobiliTudoApi.calcularRota([1, 2], [3, 4], 'PADRAO').catch(function () {});

        expect(window.MobiliTudoApi.online()).toBe(true);
    });
});

describe('calcularRota', () => {
    test('monta o corpo da requisição no formato que a API espera', async () => {
        var fetchFalso = vi.fn().mockResolvedValue(respostaFake(200, { distanciaMetros: 100 }));
        vi.stubGlobal('fetch', fetchFalso);

        await window.MobiliTudoApi.calcularRota([-23.99, -46.41], [-23.98, -46.40], 'CADEIRA_DE_RODAS');

        var chamada = fetchFalso.mock.calls[0];
        expect(chamada[0]).toBe('http://localhost:8080/api/v1/rotas/acessivel');
        var corpo = JSON.parse(chamada[1].body);
        expect(corpo).toEqual({
            origem: { lat: -23.99, lng: -46.41 },
            destino: { lat: -23.98, lng: -46.40 },
            perfil: 'CADEIRA_DE_RODAS'
        });
    });

    test('erro HTTP vira uma Error com o detail da API e o status anexado', async () => {
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue(
            respostaFake(404, { detail: 'Sem rota', motivo: 'SEM_CAMINHO_ACESSIVEL' })));

        var erroCapturado = null;
        await window.MobiliTudoApi.calcularRota([1, 2], [3, 4], 'PADRAO').catch(function (erro) {
            erroCapturado = erro;
        });

        expect(erroCapturado.message).toBe('Sem rota');
        expect(erroCapturado.status).toBe(404);
        expect(erroCapturado.corpo.motivo).toBe('SEM_CAMINHO_ACESSIVEL');
    });
});

describe('avaliarObstaculo', () => {
    test('envia o token de sessão no cabeçalho Authorization: Bearer', async () => {
        var fetchFalso = vi.fn().mockResolvedValue(respostaFake(200, { status: 'VALIDADO' }));
        vi.stubGlobal('fetch', fetchFalso);

        await window.MobiliTudoApi.avaliarObstaculo('id-123', 'VALIDAR', 'meu-token-secreto');

        var chamada = fetchFalso.mock.calls[0];
        expect(chamada[0]).toBe('http://localhost:8080/api/v1/obstaculos/id-123/validacao');
        expect(chamada[1].headers['Authorization']).toBe('Bearer meu-token-secreto');
        expect(JSON.parse(chamada[1].body)).toEqual({ acao: 'VALIDAR' });
    });

    test('sem token, envia "Bearer " vazio em vez de omitir o cabeçalho', async () => {
        // O backend trata ausência e token vazio da mesma forma (401), mas enviar o
        // cabeçalho vazio em vez de não enviar deixa explícito, do lado do cliente,
        // que a chamada sabe que precisa de uma sessão e simplesmente não tem uma —
        // útil para quem for depurar pela aba de rede do navegador.
        var fetchFalso = vi.fn().mockResolvedValue(respostaFake(401, { detail: 'sem sessão' }));
        vi.stubGlobal('fetch', fetchFalso);

        await window.MobiliTudoApi.avaliarObstaculo('id-123', 'VALIDAR', null).catch(function () {});

        var chamada = fetchFalso.mock.calls[0];
        expect(chamada[1].headers['Authorization']).toBe('Bearer ');
    });
});

describe('loginCurador', () => {
    test('envia e-mail e senha no corpo, sem exigir sessão prévia', async () => {
        var fetchFalso = vi.fn().mockResolvedValue(respostaFake(200,
            { token: 'token-de-sessao', curador: { email: 'curadora@mobilitudo.org' } }));
        vi.stubGlobal('fetch', fetchFalso);

        var resultado = await window.MobiliTudoApi.loginCurador('curadora@mobilitudo.org', 'senha-123');

        var chamada = fetchFalso.mock.calls[0];
        expect(chamada[0]).toBe('http://localhost:8080/api/v1/curadores/login');
        expect(JSON.parse(chamada[1].body)).toEqual({ email: 'curadora@mobilitudo.org', senha: 'senha-123' });
        expect(resultado.token).toBe('token-de-sessao');
    });
});

describe('logoutCurador', () => {
    test('envia o token no cabeçalho Authorization, sem corpo', async () => {
        var fetchFalso = vi.fn().mockResolvedValue(respostaFake(204, null));
        vi.stubGlobal('fetch', fetchFalso);

        await window.MobiliTudoApi.logoutCurador('meu-token-secreto');

        var chamada = fetchFalso.mock.calls[0];
        expect(chamada[0]).toBe('http://localhost:8080/api/v1/curadores/logout');
        expect(chamada[1].method).toBe('POST');
        expect(chamada[1].headers['Authorization']).toBe('Bearer meu-token-secreto');
    });
});

describe('curadorAutenticado', () => {
    test('busca /me com o token no cabeçalho Authorization', async () => {
        var fetchFalso = vi.fn().mockResolvedValue(respostaFake(200, { email: 'curadora@mobilitudo.org' }));
        vi.stubGlobal('fetch', fetchFalso);

        var curador = await window.MobiliTudoApi.curadorAutenticado('meu-token-secreto');

        var chamada = fetchFalso.mock.calls[0];
        expect(chamada[0]).toBe('http://localhost:8080/api/v1/curadores/me');
        expect(chamada[1].headers['Authorization']).toBe('Bearer meu-token-secreto');
        expect(curador.email).toBe('curadora@mobilitudo.org');
    });
});

describe('cadastrarCurador', () => {
    test('envia os dados da nova conta e o token de quem está cadastrando', async () => {
        var fetchFalso = vi.fn().mockResolvedValue(respostaFake(201, { email: 'nova@mobilitudo.org' }));
        vi.stubGlobal('fetch', fetchFalso);

        var dados = { email: 'nova@mobilitudo.org', nomeDeExibicao: 'Nova', senha: 'senha-nova-123' };
        await window.MobiliTudoApi.cadastrarCurador('meu-token-secreto', dados);

        var chamada = fetchFalso.mock.calls[0];
        expect(chamada[0]).toBe('http://localhost:8080/api/v1/curadores');
        expect(chamada[1].headers['Authorization']).toBe('Bearer meu-token-secreto');
        expect(JSON.parse(chamada[1].body)).toEqual(dados);
    });
});

describe('desativarCurador', () => {
    test('chama o endpoint de desativação com o id no caminho', async () => {
        var fetchFalso = vi.fn().mockResolvedValue(respostaFake(200, { ativo: false }));
        vi.stubGlobal('fetch', fetchFalso);

        await window.MobiliTudoApi.desativarCurador('meu-token-secreto', 'id-456');

        var chamada = fetchFalso.mock.calls[0];
        expect(chamada[0]).toBe('http://localhost:8080/api/v1/curadores/id-456/desativar');
        expect(chamada[1].method).toBe('POST');
        expect(chamada[1].headers['Authorization']).toBe('Bearer meu-token-secreto');
    });
});

describe('listarObstaculosPorStatus', () => {
    test('monta a URL com o status e um tamanho de página generoso', async () => {
        var fetchFalso = vi.fn().mockResolvedValue(respostaFake(200, []));
        vi.stubGlobal('fetch', fetchFalso);

        await window.MobiliTudoApi.listarObstaculosPorStatus('PENDENTE');

        expect(fetchFalso).toHaveBeenCalledWith(
            'http://localhost:8080/api/v1/obstaculos?status=PENDENTE&tamanho=200', expect.anything());
    });
});

describe('reportarObstaculo', () => {
    test('não exige token e envia o relato como está', async () => {
        var fetchFalso = vi.fn().mockResolvedValue(respostaFake(201, { status: 'PENDENTE' }));
        vi.stubGlobal('fetch', fetchFalso);

        var relato = { tipo: 'DEGRAU', severidade: 'MODERADA', coordenada: { lat: 1, lng: 2 } };
        await window.MobiliTudoApi.reportarObstaculo(relato);

        var chamada = fetchFalso.mock.calls[0];
        expect(chamada[1].headers['Authorization']).toBeUndefined();
        expect(JSON.parse(chamada[1].body)).toEqual(relato);
    });
});
