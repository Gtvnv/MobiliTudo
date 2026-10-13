import { afterEach, beforeAll, beforeEach, describe, expect, test, vi } from 'vitest';
import { carregarScript } from './auxiliares/carregarScript.js';

beforeAll(() => {
    carregarScript('js/texto.js');
    carregarScript('js/config.js');
});

beforeEach(() => {
    window.localStorage.clear();
    window.sessionStorage.clear();
});

afterEach(() => {
    vi.unstubAllGlobals();
});

function comLocalizacao(hostname, protocol) {
    vi.stubGlobal('location', { hostname: hostname, protocol: protocol || 'http:' });
}

describe('baseDaApi', () => {
    test('endereço configurado explicitamente tem prioridade sobre qualquer heurística', () => {
        comLocalizacao('meusite.com.br');
        window.MobiliTudoConfig.definirBaseDaApi('https://api.exemplo.org');

        expect(window.MobiliTudoConfig.baseDaApi()).toBe('https://api.exemplo.org');
    });

    test('barras finais são removidas do endereço configurado', () => {
        comLocalizacao('meusite.com.br');
        window.MobiliTudoConfig.definirBaseDaApi('https://api.exemplo.org///');

        expect(window.MobiliTudoConfig.baseDaApi()).toBe('https://api.exemplo.org');
    });

    test('em localhost, sem configuração explícita, assume a API de desenvolvimento', () => {
        comLocalizacao('localhost');
        expect(window.MobiliTudoConfig.baseDaApi()).toBe('http://localhost:8080');
    });

    test('em 127.0.0.1, sem configuração explícita, assume a API de desenvolvimento', () => {
        comLocalizacao('127.0.0.1');
        expect(window.MobiliTudoConfig.baseDaApi()).toBe('http://localhost:8080');
    });

    test('em qualquer outro host, sem configuração explícita, não adivinha uma origem', () => {
        // Adivinhar erraria na maioria das publicações reais (GitHub Pages servindo o
        // front, backend em outro domínio) — melhor admitir "sem API" do que apontar
        // para um endereço que não existe.
        comLocalizacao('meusite.com.br');
        expect(window.MobiliTudoConfig.baseDaApi()).toBeNull();
    });

    test('definirBaseDaApi com string vazia volta ao comportamento padrão', () => {
        comLocalizacao('meusite.com.br');
        window.MobiliTudoConfig.definirBaseDaApi('https://api.exemplo.org');
        window.MobiliTudoConfig.definirBaseDaApi('');

        expect(window.MobiliTudoConfig.baseDaApi()).toBeNull();
    });
});

describe('provedorPreferido', () => {
    test('sem escolha salva, não há preferido', () => {
        expect(window.MobiliTudoConfig.provedorPreferido()).toBeNull();
    });

    test('escolha salva e reconhecida é devolvida', () => {
        window.MobiliTudoConfig.definirProvedor('carto');

        var provedor = window.MobiliTudoConfig.provedorPreferido();

        expect(provedor).not.toBeNull();
        expect(provedor.id).toBe('carto');
    });

    test('id desconhecido salvo não quebra a leitura, só não é encontrado', () => {
        window.localStorage.setItem('mobilitudo.provedorMapa', 'provedor-que-nao-existe');

        expect(window.MobiliTudoConfig.provedorPreferido()).toBeNull();
    });
});

describe('abertoComoArquivoLocal', () => {
    test('protocolo file: é detectado', () => {
        comLocalizacao('', 'file:');
        expect(window.MobiliTudoConfig.abertoComoArquivoLocal()).toBe(true);
    });

    test('protocolo http: não é', () => {
        comLocalizacao('localhost', 'http:');
        expect(window.MobiliTudoConfig.abertoComoArquivoLocal()).toBe(false);
    });
});

describe('sessão de curador', () => {
    test('sem sessão definida, a leitura devolve null', () => {
        expect(window.MobiliTudoConfig.tokenDeSessao()).toBeNull();
        expect(window.MobiliTudoConfig.emailDoCurador()).toBeNull();
    });

    test('sessão definida é lida de volta', () => {
        window.MobiliTudoConfig.definirSessaoCurador('token-123', 'curadora@mobilitudo.org');

        expect(window.MobiliTudoConfig.tokenDeSessao()).toBe('token-123');
        expect(window.MobiliTudoConfig.emailDoCurador()).toBe('curadora@mobilitudo.org');
    });

    test('sessão fica em sessionStorage, não em localStorage', () => {
        // É a diferença deliberada em relação às outras preferências: não deve
        // sobreviver ao fechar a aba num computador compartilhado.
        window.MobiliTudoConfig.definirSessaoCurador('token-123', 'curadora@mobilitudo.org');

        expect(window.sessionStorage.getItem('mobilitudo.sessaoCurador.token')).toBe('token-123');
        expect(window.localStorage.getItem('mobilitudo.sessaoCurador.token')).toBeNull();
    });

    test('limparSessaoCurador remove o token e o e-mail salvos', () => {
        window.MobiliTudoConfig.definirSessaoCurador('token-123', 'curadora@mobilitudo.org');
        window.MobiliTudoConfig.limparSessaoCurador();

        expect(window.MobiliTudoConfig.tokenDeSessao()).toBeNull();
        expect(window.MobiliTudoConfig.emailDoCurador()).toBeNull();
    });
});

describe('PROVEDORES', () => {
    test('o OpenStreetMap padrão usa a URL sem subdomínios descontinuados', () => {
        var osm = window.MobiliTudoConfig.PROVEDORES.find(function (p) {
            return p.id === 'osm';
        });

        expect(osm.url).not.toContain('{s}');
        expect(osm.url).toBe('https://tile.openstreetmap.org/{z}/{x}/{y}.png');
        expect(osm.exigeReferer).toBe(true);
    });

    test('o provedor alternativo não exige Referer', () => {
        var carto = window.MobiliTudoConfig.PROVEDORES.find(function (p) {
            return p.id === 'carto';
        });

        expect(carto.exigeReferer).toBe(false);
    });
});
