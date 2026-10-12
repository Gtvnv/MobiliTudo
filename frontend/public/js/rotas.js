/**
 * Painel de rotas acessíveis.
 *
 * O trajeto é desenhado trecho a trecho, e não como uma linha só, porque a cor de cada
 * trecho carrega informação: verde onde a calçada atende ao perfil, âmbar onde passa com
 * ressalva. Uma polilinha única esconderia justamente o que o projeto quer mostrar.
 */
(function (global) {
    'use strict';

    var api = global.MobiliTudoApi;
    var config = global.MobiliTudoConfig;

    var CORES = {
        PLENO: '#1a8a3f',
        COM_RESSALVAS: '#d98200',
        INACESSIVEL: '#c62828'
    };

    var mapa = null;
    var camadaRota = null;
    var marcadorOrigem = null;
    var marcadorDestino = null;
    var origem = null;
    var destino = null;
    var aguardandoCliqueDeOrigem = false;

    function elemento(id) {
        return document.getElementById(id);
    }

    function perfilSelecionado() {
        var selecao = elemento('perfilRota');
        return selecao ? selecao.value : config.PERFIL_PADRAO;
    }

    function formatarDistancia(metros) {
        return metros >= 1000
            ? (metros / 1000).toFixed(2).replace('.', ',') + ' km'
            : Math.round(metros) + ' m';
    }

    function formatarDuracao(minutos) {
        if (minutos < 1) {
            return 'menos de 1 min';
        }
        return Math.round(minutos) + ' min';
    }

    function abrirPainel() {
        var painel = elemento('painelRota');
        if (painel) {
            painel.classList.add('aberto');
        }
    }

    function fecharPainel() {
        var painel = elemento('painelRota');
        if (painel) {
            painel.classList.remove('aberto');
        }
    }

    function limparTrajeto() {
        if (camadaRota) {
            mapa.removeLayer(camadaRota);
            camadaRota = null;
        }
    }

    function marcar(coordenada, rotulo, existente) {
        if (existente) {
            mapa.removeLayer(existente);
        }
        return L.circleMarker(coordenada, {
            radius: 7,
            color: '#fff',
            weight: 2,
            fillColor: '#0056b3',
            fillOpacity: 1
        }).addTo(mapa).bindTooltip(rotulo);
    }

    function definirOrigem(coordenada, rotulo) {
        origem = coordenada;
        marcadorOrigem = marcar(coordenada, rotulo || 'Origem', marcadorOrigem);
        var campo = elemento('origemRota');
        if (campo) {
            campo.textContent = rotulo || (coordenada[0].toFixed(5) + ', ' + coordenada[1].toFixed(5));
        }
    }

    function definirDestino(coordenada, rotulo) {
        destino = coordenada;
        marcadorDestino = marcar(coordenada, rotulo || 'Destino', marcadorDestino);
        var campo = elemento('destinoRota');
        if (campo) {
            campo.textContent = rotulo || (coordenada[0].toFixed(5) + ', ' + coordenada[1].toFixed(5));
        }
    }

    function usarMinhaLocalizacaoComoOrigem() {
        if (!navigator.geolocation) {
            mostrarMensagem('Seu navegador não oferece geolocalização.', 'alerta');
            return;
        }
        mostrarMensagem('Obtendo sua localização...', 'info');
        navigator.geolocation.getCurrentPosition(function (posicao) {
            definirOrigem([posicao.coords.latitude, posicao.coords.longitude], 'Minha localização');
            mostrarMensagem('', 'info');
        }, function () {
            mostrarMensagem('Não foi possível obter sua localização. Verifique as permissões.', 'alerta');
        }, { enableHighAccuracy: true });
    }

    function escolherOrigemNoMapa() {
        aguardandoCliqueDeOrigem = true;
        mostrarMensagem('Toque no mapa para marcar o ponto de partida.', 'info');
    }

    function aoClicarNoMapa(evento) {
        if (!aguardandoCliqueDeOrigem) {
            return;
        }
        aguardandoCliqueDeOrigem = false;
        definirOrigem([evento.latlng.lat, evento.latlng.lng], null);
        mostrarMensagem('', 'info');
    }

    function mostrarMensagem(texto, tipo) {
        var caixa = elemento('mensagemRota');
        if (!caixa) {
            return;
        }
        caixa.textContent = texto;
        caixa.className = 'mensagem-rota ' + (tipo || 'info');
        caixa.hidden = !texto;
    }

    function desenhar(rota) {
        limparTrajeto();
        camadaRota = L.layerGroup().addTo(mapa);

        // A API devolve a geometria completa; os passos trazem o nível de cada trecho.
        // Como a geometria é contínua, cada passo consome o próximo pedaço da linha.
        var pontos = rota.geometria;
        if (!pontos || pontos.length < 2) {
            return;
        }
        var porPasso = Math.max(1, Math.floor((pontos.length - 1) / Math.max(1, rota.passos.length)));
        var inicio = 0;

        rota.passos.forEach(function (passo, indice) {
            var fim = indice === rota.passos.length - 1 ? pontos.length - 1 : Math.min(pontos.length - 1, inicio + porPasso);
            var trecho = pontos.slice(inicio, fim + 1);
            if (trecho.length >= 2) {
                L.polyline(trecho, {
                    color: CORES[passo.nivelAcessibilidade] || CORES.COM_RESSALVAS,
                    weight: 6,
                    opacity: 0.85
                }).addTo(camadaRota).bindTooltip(passo.instrucao);
            }
            inicio = fim;
        });

        mapa.fitBounds(L.latLngBounds(pontos), { padding: [60, 60] });
    }

    function renderizarResultado(rota) {
        var destinoHtml = elemento('resultadoRota');
        if (!destinoHtml) {
            return;
        }
        var passos = rota.passos.map(function (passo) {
            var ressalvas = passo.ressalvas.length
                ? '<ul class="ressalvas">' + passo.ressalvas.map(function (r) {
                    return '<li>' + r + '</li>';
                }).join('') + '</ul>'
                : '';
            return '<li class="passo nivel-' + passo.nivelAcessibilidade.toLowerCase() + '">'
                + '<span class="passo-texto">' + passo.instrucao + '</span>' + ressalvas + '</li>';
        }).join('');

        destinoHtml.innerHTML =
            '<div class="resumo-rota nivel-' + rota.nivelAcessibilidade.toLowerCase() + '">'
            + '<strong>' + rota.nivelDescricao + '</strong>'
            + '<span>' + formatarDistancia(rota.distanciaMetros) + ' · '
            + formatarDuracao(rota.duracaoMinutos) + ' · ' + rota.perfilDescricao + '</span>'
            + '</div>'
            + '<ol class="passos-rota">' + passos + '</ol>';
        destinoHtml.hidden = false;
    }

    var MOTIVOS = {
        ORIGEM_FORA_DA_AREA_MAPEADA: 'A origem está fora do trecho já mapeado de Praia Grande.',
        DESTINO_FORA_DA_AREA_MAPEADA: 'O destino está fora do trecho já mapeado de Praia Grande.',
        SEM_CAMINHO_ACESSIVEL: 'Existe caminho até lá, mas nenhum que este perfil consiga vencer. '
            + 'Vale reportar a barreira que isola o destino.'
    };

    function calcular() {
        if (!api.configurada()) {
            mostrarMensagem('O cálculo de rotas precisa do backend. Configure o endereço da API em Configurações.', 'alerta');
            return;
        }
        if (!origem || !destino) {
            mostrarMensagem('Escolha o ponto de partida e o destino.', 'alerta');
            return;
        }
        mostrarMensagem('Calculando a melhor rota para o seu perfil...', 'info');

        api.calcularRota(origem, destino, perfilSelecionado())
            .then(function (rota) {
                mostrarMensagem('', 'info');
                desenhar(rota);
                renderizarResultado(rota);
            })
            .catch(function (erro) {
                var motivo = erro.corpo && erro.corpo.motivo;
                mostrarMensagem(MOTIVOS[motivo] || ('Não foi possível calcular a rota: ' + erro.message), 'alerta');
                var resultado = elemento('resultadoRota');
                if (resultado) {
                    resultado.hidden = true;
                }
            });
    }

    function iniciar(instanciaDoMapa) {
        mapa = instanciaDoMapa;
        mapa.on('click', aoClicarNoMapa);

        var perfilSalvo = null;
        try {
            perfilSalvo = localStorage.getItem(config.chavePerfil);
        } catch (erro) {
            perfilSalvo = null;
        }
        var selecao = elemento('perfilRota');
        if (selecao && perfilSalvo) {
            selecao.value = perfilSalvo;
        }
        if (selecao) {
            selecao.addEventListener('change', function () {
                try {
                    localStorage.setItem(config.chavePerfil, selecao.value);
                } catch (erro) {
                    /* preferência vale só nesta sessão */
                }
            });
        }
    }

    global.MobiliTudoRotas = {
        iniciar: iniciar,
        abrirPainel: abrirPainel,
        fecharPainel: fecharPainel,
        definirOrigem: definirOrigem,
        definirDestino: definirDestino,
        usarMinhaLocalizacaoComoOrigem: usarMinhaLocalizacaoComoOrigem,
        escolherOrigemNoMapa: escolherOrigemNoMapa,
        calcular: calcular,
        limparTrajeto: limparTrajeto,
        perfilSelecionado: perfilSelecionado
    };
}(window));
