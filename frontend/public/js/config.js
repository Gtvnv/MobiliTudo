/**
 * Configuração central do MobiliTudo.
 *
 * Reúne o que muda entre ambientes — endereço da API e provedor de tiles — em um único
 * lugar, para que nenhum outro arquivo precise cravar URL.
 */
(function (global) {
    'use strict';

    var CHAVE_API = 'mobilitudo.apiBase';
    var CHAVE_PROVEDOR = 'mobilitudo.provedorMapa';

    /**
     * Provedores de tiles, em ordem de preferência.
     *
     * O padrão do OpenStreetMap é o primeiro por ser o mais fiel ao projeto, mas ele exige
     * um Referer válido (ver docs/mapa-tiles.md). Quando ele falha — página aberta por
     * file://, IP compartilhado bloqueado — o mapa cai para o provedor seguinte em vez de
     * ficar exibindo o quadriculado de erro.
     */
    var PROVEDORES = [
        {
            id: 'osm',
            nome: 'OpenStreetMap',
            // Sem o prefixo {s}: os subdomínios a/b/c.tile.openstreetmap.org foram
            // descontinuados e usá-los é, por si só, violação da política de uso.
            url: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
            opcoes: {
                maxZoom: 19,
                attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            },
            exigeReferer: true
        },
        {
            id: 'carto',
            nome: 'CARTO Voyager',
            url: 'https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png',
            opcoes: {
                maxZoom: 20,
                subdomains: 'abcd',
                attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>'
            },
            exigeReferer: false
        }
    ];

    function ler(chave) {
        try {
            return global.localStorage.getItem(chave);
        } catch (erro) {
            return null;
        }
    }

    function gravar(chave, valor) {
        try {
            if (valor === null || valor === undefined || valor === '') {
                global.localStorage.removeItem(chave);
            } else {
                global.localStorage.setItem(chave, valor);
            }
        } catch (erro) {
            /* modo privado ou armazenamento bloqueado: a configuração vale só nesta sessão */
        }
    }

    /**
     * Endereço da API.
     *
     * Em desenvolvimento o backend sobe ao lado, na 8080. Em qualquer outro host só vale
     * o que tiver sido configurado explicitamente — sem isso, o mapa opera offline com o
     * acervo local, que é melhor do que tentar uma origem adivinhada e falhar.
     */
    function baseDaApi() {
        var configurada = ler(CHAVE_API);
        if (configurada) {
            return configurada.replace(/\/+$/, '');
        }
        var host = global.location.hostname;
        if (host === 'localhost' || host === '127.0.0.1') {
            return 'http://localhost:8080';
        }
        return null;
    }

    function provedorPreferido() {
        var escolhido = ler(CHAVE_PROVEDOR);
        for (var i = 0; i < PROVEDORES.length; i++) {
            if (PROVEDORES[i].id === escolhido) {
                return PROVEDORES[i];
            }
        }
        return null;
    }

    /** Páginas abertas direto do disco não enviam Referer, e o OSM bloqueia essas requisições. */
    function abertoComoArquivoLocal() {
        return global.location.protocol === 'file:';
    }

    var CHAVE_TOKEN_SESSAO = 'mobilitudo.sessaoCurador.token';
    var CHAVE_EMAIL_CURADOR = 'mobilitudo.sessaoCurador.email';

    /**
     * A sessão de curador (token de login + e-mail da conta) fica em sessionStorage,
     * não em localStorage.
     *
     * É um segredo, ainda que de posse temporária: diferente do perfil de mobilidade ou
     * do provedor de mapa, não deve sobreviver ao fechar a aba num computador que outra
     * pessoa também usa. sessionStorage soma isso de graça — expira com a aba — sem
     * exigir lógica própria de expiração aqui.
     */
    function lerDeSessao(chave) {
        try {
            return global.sessionStorage.getItem(chave);
        } catch (erro) {
            return null;
        }
    }

    function gravarEmSessao(chave, valor) {
        try {
            if (valor) {
                global.sessionStorage.setItem(chave, valor);
            } else {
                global.sessionStorage.removeItem(chave);
            }
        } catch (erro) {
            /* sessão privada ou armazenamento bloqueado: a sessão vale só nesta chamada */
        }
    }

    function tokenDeSessao() {
        return lerDeSessao(CHAVE_TOKEN_SESSAO);
    }

    function emailDoCurador() {
        return lerDeSessao(CHAVE_EMAIL_CURADOR);
    }

    function definirSessaoCurador(token, email) {
        gravarEmSessao(CHAVE_TOKEN_SESSAO, token);
        gravarEmSessao(CHAVE_EMAIL_CURADOR, email);
    }

    function limparSessaoCurador() {
        gravarEmSessao(CHAVE_TOKEN_SESSAO, null);
        gravarEmSessao(CHAVE_EMAIL_CURADOR, null);
    }

    global.MobiliTudoConfig = {
        PROVEDORES: PROVEDORES,
        baseDaApi: baseDaApi,
        definirBaseDaApi: function (valor) {
            gravar(CHAVE_API, valor);
        },
        provedorPreferido: provedorPreferido,
        definirProvedor: function (id) {
            gravar(CHAVE_PROVEDOR, id);
        },
        abertoComoArquivoLocal: abertoComoArquivoLocal,
        PERFIL_PADRAO: 'PADRAO',
        chavePerfil: 'mobilitudo.perfil',
        tokenDeSessao: tokenDeSessao,
        emailDoCurador: emailDoCurador,
        definirSessaoCurador: definirSessaoCurador,
        limparSessaoCurador: limparSessaoCurador
    };
}(window));
