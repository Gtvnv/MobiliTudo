/**
 * Cliente da API do MobiliTudo, com degradação para o acervo local.
 *
 * O mapa continua funcionando sem backend: locais.json segue sendo a fonte de dados
 * quando a API não está configurada ou não responde. O que exige servidor — rotas e
 * envio de relatos — fica indisponível e diz isso, em vez de falhar em silêncio.
 */
(function (global) {
    'use strict';

    var config = global.MobiliTudoConfig;
    var TEMPO_LIMITE_MS = 8000;

    var estado = {
        online: false,
        verificada: false
    };

    function url(caminho) {
        var base = config.baseDaApi();
        return base ? base + caminho : null;
    }

    function requisitar(caminho, opcoes) {
        var alvo = url(caminho);
        if (!alvo) {
            return Promise.reject(new Error('API não configurada'));
        }
        var controlador = new AbortController();
        var expirou = setTimeout(function () {
            controlador.abort();
        }, TEMPO_LIMITE_MS);

        var parametros = Object.assign({ headers: { 'Accept': 'application/json' } }, opcoes || {});
        parametros.signal = controlador.signal;

        return fetch(alvo, parametros)
            .then(function (resposta) {
                clearTimeout(expirou);
                if (!resposta.ok) {
                    return resposta.json().catch(function () {
                        return {};
                    }).then(function (corpo) {
                        var erro = new Error(corpo.detail || ('HTTP ' + resposta.status));
                        erro.status = resposta.status;
                        erro.corpo = corpo;
                        throw erro;
                    });
                }
                estado.online = true;
                return resposta.status === 204 ? null : resposta.json();
            })
            .catch(function (erro) {
                clearTimeout(expirou);
                // Falha de transporte derruba o modo online; erro de regra (4xx) não.
                if (!erro.status) {
                    estado.online = false;
                }
                throw erro;
            });
    }

    function enviarJson(caminho, corpo, cabecalhosExtras) {
        return requisitar(caminho, {
            method: 'POST',
            headers: Object.assign(
                { 'Content-Type': 'application/json', 'Accept': 'application/json' },
                cabecalhosExtras || {}),
            body: JSON.stringify(corpo)
        });
    }

    /** Converte o locais.json local para o mesmo formato que a API devolve. */
    function normalizarLocalLocal(bruto) {
        return {
            id: null,
            nome: bruto.nome,
            categoria: bruto.categoria,
            coords: bruto.coords,
            info: bruto.info,
            imagem: bruto.imagem,
            recursos: bruto.recursos || [],
            nivelAcessibilidade: null,
            nivelDescricao: null
        };
    }

    function carregarAcervoLocal() {
        return fetch('locais.json')
            .then(function (resposta) {
                if (!resposta.ok) {
                    throw new Error('Não foi possível ler locais.json');
                }
                return resposta.json();
            })
            .then(function (dados) {
                return dados.map(normalizarLocalLocal);
            });
    }

    /**
     * Carrega os locais, preferindo a API e caindo para o arquivo local.
     *
     * @param {string} perfil perfil de mobilidade para classificar cada local
     */
    function listarLocais(perfil) {
        var consulta = perfil && perfil !== config.PERFIL_PADRAO ? '?perfil=' + encodeURIComponent(perfil) : '';
        return requisitar('/api/v1/locais' + consulta)
            .catch(function () {
                estado.online = false;
                return carregarAcervoLocal();
            })
            .then(function (locais) {
                estado.verificada = true;
                return locais;
            });
    }

    function calcularRota(origem, destino, perfil) {
        return enviarJson('/api/v1/rotas/acessivel', {
            origem: { lat: origem[0], lng: origem[1] },
            destino: { lat: destino[0], lng: destino[1] },
            perfil: perfil
        });
    }

    function listarObstaculos(centro, raioMetros) {
        var consulta = '?lat=' + centro[0] + '&lng=' + centro[1] + '&raio=' + (raioMetros || 3000);
        return requisitar('/api/v1/obstaculos' + consulta);
    }

    /**
     * Lista obstáculos por status, para a fila de curadoria.
     *
     * Busca uma única página grande (o máximo que a API aceita) em vez de paginar de
     * verdade: a fila de pendências tende a ser pequena, e um painel de curadoria sem
     * "carregar mais" é simples o bastante para não precisar de paginação agora. Se a
     * fila crescer muito, é aqui que entra.
     */
    function listarObstaculosPorStatus(status) {
        return requisitar('/api/v1/obstaculos?status=' + encodeURIComponent(status) + '&tamanho=200');
    }

    function reportarObstaculo(relato) {
        return enviarJson('/api/v1/obstaculos', relato);
    }

    /**
     * Valida, rejeita ou resolve um obstáculo. Exige o token de curadoria configurado em
     * Configurações — sem ele, a API responde 401 ou 503, e o chamador trata o erro como
     * qualquer outra falha de requisição.
     */
    function avaliarObstaculo(id, acao, tokenCurador) {
        return enviarJson('/api/v1/obstaculos/' + encodeURIComponent(id) + '/validacao',
            { acao: acao },
            { 'X-Mobilitudo-Curador-Token': tokenCurador || '' });
    }

    /**
     * Remove o nome de quem relatou um obstáculo, usando o token de exclusão devolvido
     * na criação (ver reportarObstaculo). Não exige token de curadoria — é
     * autoexclusão de dado pessoal, baseada em posse do token, não em autenticação.
     */
    function anonimizarObstaculo(id, tokenExclusao) {
        return enviarJson('/api/v1/obstaculos/' + encodeURIComponent(id) + '/anonimizar',
            { tokenExclusao: tokenExclusao });
    }

    function metadados() {
        return requisitar('/api/v1/metadados');
    }

    global.MobiliTudoApi = {
        configurada: function () {
            return config.baseDaApi() !== null;
        },
        online: function () {
            return estado.online;
        },
        listarLocais: listarLocais,
        calcularRota: calcularRota,
        listarObstaculos: listarObstaculos,
        listarObstaculosPorStatus: listarObstaculosPorStatus,
        reportarObstaculo: reportarObstaculo,
        avaliarObstaculo: avaliarObstaculo,
        anonimizarObstaculo: anonimizarObstaculo,
        metadados: metadados
    };
}(window));
