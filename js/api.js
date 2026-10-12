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

    function enviarJson(caminho, corpo) {
        return requisitar(caminho, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
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

    function reportarObstaculo(relato) {
        return enviarJson('/api/v1/obstaculos', relato);
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
        reportarObstaculo: reportarObstaculo,
        metadados: metadados
    };
}(window));
