/**
 * Relato colaborativo de barreiras urbanas.
 *
 * Os obstáculos validados aparecem no mapa com cor por severidade; os pendentes aparecem
 * esmaecidos. Mostrar os dois deixa visível que o relato de alguém foi recebido, mesmo
 * antes de a curadoria confirmá-lo.
 */
(function (global) {
    'use strict';

    var api = global.MobiliTudoApi;

    var CORES = {
        LEVE: '#f1b24a',
        MODERADA: '#e07b2a',
        CRITICA: '#c62828'
    };

    var mapa = null;
    var camada = null;
    var pontoSelecionado = null;
    var marcadorTemporario = null;
    var aguardandoCliqueNoMapa = false;

    function elemento(id) {
        return document.getElementById(id);
    }

    function mensagem(texto, tipo) {
        var caixa = elemento('mensagemObstaculo');
        if (!caixa) {
            return;
        }
        caixa.textContent = texto;
        caixa.className = 'mensagem-rota ' + (tipo || 'info');
        caixa.hidden = !texto;
    }

    function desenhar(obstaculos) {
        if (camada) {
            mapa.removeLayer(camada);
        }
        camada = L.layerGroup().addTo(mapa);

        obstaculos.forEach(function (obstaculo) {
            var validado = obstaculo.status === 'VALIDADO';
            L.circleMarker(obstaculo.coords, {
                radius: validado ? 9 : 7,
                color: '#fff',
                weight: 2,
                fillColor: CORES[obstaculo.severidade] || CORES.MODERADA,
                fillOpacity: validado ? 0.95 : 0.45,
                dashArray: validado ? null : '3'
            }).addTo(camada).bindPopup(
                '<strong>' + obstaculo.tipoDescricao + '</strong><br>'
                + (obstaculo.descricao ? obstaculo.descricao + '<br>' : '')
                + '<small>' + (validado ? 'Confirmado — já desvia rotas' : 'Aguardando conferência') + '</small>'
            );
        });
    }

    function carregar(centro) {
        if (!api.configurada()) {
            return Promise.resolve([]);
        }
        return api.listarObstaculos(centro, 4000)
            .then(function (obstaculos) {
                desenhar(obstaculos);
                return obstaculos;
            })
            .catch(function () {
                // Sem backend não há relatos a exibir; o mapa segue com os locais.
                return [];
            });
    }

    function abrirFormulario() {
        if (!api.configurada()) {
            mensagem('Para enviar relatos é preciso configurar o endereço da API em Configurações.', 'alerta');
        }
        var painel = elemento('painelObstaculo');
        if (painel) {
            painel.classList.add('aberto');
        }
        aguardandoCliqueNoMapa = true;
        mensagem('Toque no mapa, sobre a barreira que você quer reportar.', 'info');
    }

    function fecharFormulario() {
        var painel = elemento('painelObstaculo');
        if (painel) {
            painel.classList.remove('aberto');
        }
        aguardandoCliqueNoMapa = false;
        if (marcadorTemporario) {
            mapa.removeLayer(marcadorTemporario);
            marcadorTemporario = null;
        }
        pontoSelecionado = null;
    }

    function aoClicarNoMapa(evento) {
        if (!aguardandoCliqueNoMapa) {
            return;
        }
        pontoSelecionado = [evento.latlng.lat, evento.latlng.lng];
        if (marcadorTemporario) {
            mapa.removeLayer(marcadorTemporario);
        }
        marcadorTemporario = L.circleMarker(pontoSelecionado, {
            radius: 9,
            color: '#c62828',
            weight: 3,
            fillColor: '#fff',
            fillOpacity: 0.9
        }).addTo(mapa);

        var campo = elemento('coordObstaculo');
        if (campo) {
            campo.textContent = pontoSelecionado[0].toFixed(5) + ', ' + pontoSelecionado[1].toFixed(5);
        }
        mensagem('Ponto marcado. Descreva a barreira e envie.', 'info');
    }

    function enviar() {
        if (!pontoSelecionado) {
            mensagem('Marque no mapa onde está a barreira.', 'alerta');
            return;
        }
        var relato = {
            tipo: elemento('tipoObstaculo').value,
            severidade: elemento('severidadeObstaculo').value,
            coordenada: { lat: pontoSelecionado[0], lng: pontoSelecionado[1] },
            descricao: elemento('descricaoObstaculo').value,
            reportadoPor: lerNomeDoUsuario()
        };

        mensagem('Enviando relato...', 'info');
        api.reportarObstaculo(relato)
            .then(function () {
                mensagem('Relato enviado. Ele entra na fila de conferência antes de desviar rotas.', 'sucesso');
                elemento('descricaoObstaculo').value = '';
                carregar(pontoSelecionado);
                setTimeout(fecharFormulario, 2500);
            })
            .catch(function (erro) {
                mensagem('Não foi possível enviar: ' + erro.message, 'alerta');
            });
    }

    function lerNomeDoUsuario() {
        try {
            return localStorage.getItem('nomeUsuario') || null;
        } catch (erro) {
            return null;
        }
    }

    function preencherTipos(metadados) {
        var selecaoTipo = elemento('tipoObstaculo');
        if (!selecaoTipo || !metadados || !metadados.tiposDeObstaculo) {
            return;
        }
        selecaoTipo.innerHTML = metadados.tiposDeObstaculo.map(function (tipo) {
            return '<option value="' + tipo.id + '">' + tipo.rotulo + '</option>';
        }).join('');
    }

    function iniciar(instanciaDoMapa) {
        mapa = instanciaDoMapa;
        mapa.on('click', aoClicarNoMapa);

        if (api.configurada()) {
            api.metadados().then(preencherTipos).catch(function () {
                // Sem metadados, a lista fixa do HTML continua servindo.
            });
        }
    }

    global.MobiliTudoObstaculos = {
        iniciar: iniciar,
        carregar: carregar,
        abrirFormulario: abrirFormulario,
        fecharFormulario: fecharFormulario,
        enviar: enviar
    };
}(window));
