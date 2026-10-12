/**
 * Camada base do mapa, com tolerância a falha do provedor de tiles.
 *
 * Resolve o erro 403 "Access blocked" do OpenStreetMap. Três causas, nesta ordem de
 * frequência (detalhes em docs/mapa-tiles.md):
 *
 *   1. página aberta por file://, que não manda Referer algum;
 *   2. URL antiga com {s}.tile.openstreetmap.org, cujos subdomínios foram descontinuados;
 *   3. atribuição ausente ou volume de requisições acima do que a política permite.
 *
 * A (2) e a (3) estão corrigidas em js/config.js. A (1) não tem conserto dentro da
 * página — nenhum JavaScript inventa um Referer — então aqui ela é detectada antes da
 * primeira requisição e o mapa já sobe num provedor que não exige Referer, avisando
 * quem abriu o arquivo de que o jeito certo é servir por HTTP.
 */
(function (global) {
    'use strict';

    var config = global.MobiliTudoConfig;

    /** Erros de tile tolerados antes de desistir do provedor atual. */
    var LIMITE_DE_ERROS = 4;

    /** Depois disso, erros esparsos são rede instável, não bloqueio. */
    var JANELA_DE_AVALIACAO_MS = 15000;

    function criarCamada(provedor) {
        return L.tileLayer(provedor.url, provedor.opcoes);
    }

    /**
     * Avisos ativos, por chave.
     *
     * Uma caixa unica com a ultima mensagem perdia justamente o aviso que mais importa:
     * aberta por file://, a explicacao de como servir por HTTP era sobrescrita segundos
     * depois pelo erro de carga dos locais, que e consequencia do mesmo problema.
     */
    var avisos = {};

    function redesenharAvisos() {
        var caixa = document.getElementById('avisoMapa');
        if (!caixa) {
            return;
        }
        var chaves = Object.keys(avisos);
        if (chaves.length === 0) {
            caixa.className = 'aviso-mapa';
            caixa.innerHTML = '';
            caixa.hidden = true;
            return;
        }
        // Se qualquer aviso for de alerta, a caixa inteira assume o tom de alerta.
        var tom = 'info';
        var partes = chaves.map(function (chave) {
            if (avisos[chave].tipo === 'alerta') {
                tom = 'alerta';
            }
            return '<p class="linha-aviso">' + avisos[chave].mensagem + '</p>';
        });
        caixa.innerHTML = partes.join('');
        caixa.className = 'aviso-mapa ' + tom + ' visivel';
        caixa.hidden = false;
    }

    function mostrarAviso(mensagem, tipo, chave) {
        if (!mensagem) {
            esconderAviso(chave);
            return;
        }
        avisos[chave || 'geral'] = { mensagem: mensagem, tipo: tipo || 'info' };
        redesenharAvisos();
    }

    /** Sem chave, limpa todos os avisos. */
    function esconderAviso(chave) {
        if (chave) {
            delete avisos[chave];
        } else {
            avisos = {};
        }
        redesenharAvisos();
    }

    /**
     * Instala a camada base e devolve um controlador capaz de trocar de provedor.
     *
     * @param {L.Map} mapa mapa Leaflet já criado
     */
    function instalarCamadaBase(mapa) {
        var provedores = config.PROVEDORES.slice();
        var preferido = config.provedorPreferido();

        if (preferido) {
            // A escolha explícita do usuário vem na frente da ordem padrão.
            provedores = [preferido].concat(provedores.filter(function (p) {
                return p.id !== preferido.id;
            }));
        } else if (config.abertoComoArquivoLocal()) {
            // Sem Referer, o provedor que o exige responderia 403 em todos os tiles.
            provedores = provedores.filter(function (p) {
                return !p.exigeReferer;
            }).concat(provedores.filter(function (p) {
                return p.exigeReferer;
            }));
        }

        var indice = 0;
        var camada = null;
        var erros = 0;
        var inicioDaJanela = 0;

        function aoFalharTile() {
            var agora = Date.now();
            if (agora - inicioDaJanela > JANELA_DE_AVALIACAO_MS) {
                inicioDaJanela = agora;
                erros = 0;
            }
            erros += 1;
            if (erros >= LIMITE_DE_ERROS && indice + 1 < provedores.length) {
                var anterior = provedores[indice];
                indice += 1;
                aplicar();
                mostrarAviso('O provedor <strong>' + anterior.nome + '</strong> recusou as imagens do mapa. '
                    + 'Exibindo <strong>' + provedores[indice].nome + '</strong> no lugar.',
                    'alerta', 'provedor');
            }
        }

        function aplicar() {
            if (camada) {
                mapa.removeLayer(camada);
            }
            erros = 0;
            inicioDaJanela = Date.now();
            camada = criarCamada(provedores[indice]);
            camada.on('tileerror', aoFalharTile);
            camada.addTo(mapa);
        }

        aplicar();

        if (config.abertoComoArquivoLocal()) {
            mostrarAviso('Esta página foi aberta direto do disco (<code>file://</code>). '
                + 'Os servidores do OpenStreetMap recusam requisições sem Referer, então o mapa está '
                + 'usando <strong>' + provedores[indice].nome + '</strong>. '
                + 'Para o mapa padrão, sirva a pasta por HTTP: <code>npx serve .</code> ou '
                + '<code>python -m http.server</code>.', 'alerta', 'arquivo-local');
        }

        return {
            provedorAtual: function () {
                return provedores[indice];
            },
            trocarPara: function (id) {
                for (var i = 0; i < provedores.length; i++) {
                    if (provedores[i].id === id) {
                        indice = i;
                        aplicar();
                        esconderAviso('provedor');
                        return true;
                    }
                }
                return false;
            }
        };
    }

    global.MobiliTudoMapa = {
        instalarCamadaBase: instalarCamadaBase,
        mostrarAviso: mostrarAviso,
        esconderAviso: esconderAviso
    };
}(window));
