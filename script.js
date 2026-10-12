/**
 * MobiliTudo - orquestração do mapa.
 *
 * As funções chamadas pelos atributos onclick do HTML continuam globais, de propósito:
 * trocar isso exigiria reescrever as quatro páginas de uma vez. O que mudou é que o
 * trabalho pesado saiu daqui para os módulos em js/ (configuração, camada base, API,
 * rotas e obstáculos).
 */

var locais = [];
var marcadores = [];
var categoriaAtiva = 'todos';
var localSelecionado = null;
var mapa = null;
var camadaBase = null;

// --------------------------------------------------------------------------
// 1. Mapa
// --------------------------------------------------------------------------

mapa = L.map('map', { zoomControl: false }).setView([-23.9925, -46.4095], 15);
L.control.zoom({ position: 'topright' }).addTo(mapa);

// A camada base trata sozinha o 403 do OpenStreetMap (ver js/mapa.js).
camadaBase = MobiliTudoMapa.instalarCamadaBase(mapa);

MobiliTudoRotas.iniciar(mapa);
MobiliTudoObstaculos.iniciar(mapa);

// --------------------------------------------------------------------------
// 2. Dados
// --------------------------------------------------------------------------

function perfilAtual() {
    return MobiliTudoRotas.perfilSelecionado();
}

function carregarLocais() {
    return MobiliTudoApi.listarLocais(perfilAtual())
        .then(function (dados) {
            locais = dados;
            filtrarPor(categoriaAtiva);
            atualizarIndicadorDeConexao();
            return dados;
        })
        .catch(function (erro) {
            console.error('Falha ao carregar o acervo de locais:', erro);
            // Por file:// o navegador também bloqueia a leitura do locais.json, pela mesma
            // razão que derruba os tiles: não há origem HTTP. Dizer isso evita mandar quem
            // abriu o arquivo procurar um defeito que não existe.
            var detalhe = MobiliTudoConfig.abertoComoArquivoLocal()
                ? 'Não foi possível ler <code>locais.json</code>: o navegador bloqueia leitura de arquivos locais. Sirva a pasta por HTTP.'
                : 'Não foi possível carregar os locais mapeados.';
            MobiliTudoMapa.mostrarAviso(detalhe, 'alerta', 'acervo');
        });
}

function atualizarIndicadorDeConexao() {
    var selo = document.getElementById('seloConexao');
    if (!selo) {
        return;
    }
    if (MobiliTudoApi.online()) {
        selo.textContent = 'API conectada';
        selo.className = 'selo-conexao online';
    } else {
        selo.textContent = 'Modo offline · acervo local';
        selo.className = 'selo-conexao offline';
    }
}

// --------------------------------------------------------------------------
// 3. Marcadores, busca e filtros
// --------------------------------------------------------------------------

function renderizarMarcadores(lista) {
    marcadores.forEach(function (marcador) {
        mapa.removeLayer(marcador);
    });
    marcadores = [];

    lista.forEach(function (local) {
        var marcador = L.marker(local.coords).addTo(mapa);
        marcador.on('click', function () {
            mostrarDetalhes(local);
        });
        marcadores.push(marcador);
    });
}

function buscarLocal() {
    var termo = document.getElementById('searchInput').value.trim().toLowerCase();
    if (!termo) {
        return;
    }
    var achado = locais.find(function (local) {
        return local.nome.toLowerCase().indexOf(termo) !== -1;
    });
    if (achado) {
        MobiliTudoMapa.esconderAviso('busca');
        mapa.flyTo(achado.coords, 17);
        mostrarDetalhes(achado);
    } else {
        MobiliTudoMapa.mostrarAviso('Nenhum local mapeado corresponde a "' + escapar(termo) + '".',
            'alerta', 'busca');
    }
}

function toggleFiltros() {
    document.getElementById('filterBar').classList.toggle('active');
}

function filtrarPor(categoria) {
    categoriaAtiva = categoria;
    var filtrados = categoria === 'todos'
        ? locais
        : locais.filter(function (local) {
            return local.categoria === categoria;
        });
    renderizarMarcadores(filtrados);

    document.querySelectorAll('.filter-chip').forEach(function (chip) {
        chip.classList.toggle('selected', chip.dataset.categoria === categoria);
    });
}

// --------------------------------------------------------------------------
// 4. Ficha do local
// --------------------------------------------------------------------------

var ICONES_DE_RECURSO = {
    'Rampas': '♿',
    'Piso Tátil': '👣',
    'Elevador': '🛗',
    'Placas Braille': '⠃',
    'Banheiro Adaptado': '🚻',
    'Vaga PCD': '🅿️',
    'Cadeira Anfíbia': '🏖️',
    'Corrimão': '🤝',
    'Atendimento Prioritário': '⏱️',
    'Intérprete de Libras': '🤟'
};

function obterIconeRecurso(recurso) {
    return ICONES_DE_RECURSO[recurso] || '✅';
}

/**
 * Escapa texto para interpolação em HTML, aspas inclusive.
 *
 * textContent sozinho cobre &, < e >, mas não as aspas — e o conteúdo aqui entra dentro
 * de atributos, onde uma aspa solta quebra a marcação.
 */
function escapar(texto) {
    return String(texto === null || texto === undefined ? '' : texto)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function mostrarDetalhes(local) {
    var container = document.getElementById('placeDetails');
    localSelecionado = local;

    var recursosHtml = (local.recursos || []).map(function (recurso) {
        return '<div class="recurso-item">'
            + '<div class="recurso-circulo" title="' + escapar(recurso) + '">'
            + obterIconeRecurso(recurso) + '</div>'
            + '<span>' + escapar(recurso) + '</span></div>';
    }).join('');

    // O nível só existe quando os dados vêm da API, que o calcula para o perfil escolhido.
    var selo = local.nivelDescricao
        ? '<span class="selo-nivel nivel-' + local.nivelAcessibilidade.toLowerCase() + '">'
            + escapar(local.nivelDescricao) + '</span>'
        : '';

    // onerror cobre as imagens de acervos externos que saem do ar com o tempo.
    var imagem = local.imagem
        ? '<img src="' + escapar(local.imagem) + '" class="place-img" alt="Foto de ' + escapar(local.nome) + '"'
            + ' onclick="expandirFoto(this)"'
            + ' onerror="this.classList.add(\'sem-imagem\'); this.removeAttribute(\'onclick\');">'
        : '';

    container.innerHTML =
        '<div class="place-container">' + imagem
        + '<div class="place-info">'
        + '<h3 id="detalheTitulo">' + escapar(local.nome) + '</h3>' + selo
        + '<div class="recursos-wrapper">' + recursosHtml + '</div>'
        + '<p id="detalheTexto">' + escapar(local.info) + '</p>'
        + (local.endereco ? '<p class="endereco">' + escapar(local.endereco) + '</p>' : '')
        + '<hr>'
        + '<button class="menu-item botao-rota" onclick="tracarRotaPara(' + local.coords[0] + ','
        + local.coords[1] + ', \'' + escapar(local.nome).replace(/'/g, '') + '\')">'
        + '🧭 Traçar rota acessível</button>'
        + '<button class="menu-item" onclick="window.open(\'https://www.google.com/maps/dir/?api=1&destination='
        + local.coords[0] + ',' + local.coords[1] + '\')">📍 Abrir no Google Maps</button>'
        + '</div></div>';

    abrirMenu();
}

function tracarRotaParaLocalSelecionado() {
    if (!localSelecionado) {
        return;
    }
    MobiliTudoRotas.definirDestino(localSelecionado.coords, localSelecionado.nome);
    MobiliTudoRotas.abrirPainel();
    fecharMenu();
}

// --------------------------------------------------------------------------
// 5. Menu lateral
// --------------------------------------------------------------------------

function abrirMenu() {
    document.getElementById('sideMenu').style.width = '350px';
}

function fecharMenu() {
    document.getElementById('sideMenu').style.width = '0';
}

function toggleMenu() {
    var menu = document.getElementById('sideMenu');
    if (menu.style.width === '350px') {
        fecharMenu();
    } else {
        abrirMenu();
    }
}

function toggleSubMenu(id) {
    var sub = document.getElementById(id);
    sub.style.display = sub.style.display === 'block' ? 'none' : 'block';
}

// --------------------------------------------------------------------------
// 6. Geolocalização
// --------------------------------------------------------------------------

function minhaLocalizacao() {
    var botao = document.getElementById('btn-geoloc');
    if (!navigator.geolocation) {
        MobiliTudoMapa.mostrarAviso('Seu navegador não oferece geolocalização.', 'alerta', 'geoloc');
        return;
    }
    botao.textContent = '⏳';

    navigator.geolocation.getCurrentPosition(function (posicao) {
        var ponto = [posicao.coords.latitude, posicao.coords.longitude];
        mapa.flyTo(ponto, 17);
        L.circleMarker(ponto, {
            radius: 8, fillColor: '#4285F4', color: '#fff', weight: 2, opacity: 1, fillOpacity: 0.8
        }).addTo(mapa).bindPopup('Você está aqui!').openPopup();

        MobiliTudoRotas.definirOrigem(ponto, 'Minha localização');
        MobiliTudoObstaculos.carregar(ponto);
        botao.textContent = '🎯';
    }, function () {
        MobiliTudoMapa.mostrarAviso('Não foi possível obter sua localização. Verifique as permissões do navegador.',
            'alerta', 'geoloc');
        botao.textContent = '🎯';
    }, { enableHighAccuracy: true });
}

// --------------------------------------------------------------------------
// 7. Recursos de acessibilidade da própria interface
// --------------------------------------------------------------------------

function ativarLeitor() {
    if (!('speechSynthesis' in window)) {
        MobiliTudoMapa.mostrarAviso('Seu navegador não oferece leitura em voz alta.', 'alerta', 'voz');
        return;
    }
    var titulo = document.getElementById('detalheTitulo');
    var texto = document.getElementById('detalheTexto');
    var fala = new SpeechSynthesisUtterance(
        (titulo ? titulo.innerText : 'MobiliTudo') + '. '
        + (texto ? texto.innerText : 'Selecione um local no mapa.')
    );
    fala.lang = 'pt-BR';
    window.speechSynthesis.cancel();
    window.speechSynthesis.speak(fala);
}

function toggleContraste() {
    var ativo = document.body.classList.toggle('alto-contraste');
    gravarPreferencia('altoContraste', ativo ? 'true' : 'false');
}

/**
 * Alterna a fonte de leitura ampliada.
 *
 * O botão já existia no menu, mas a função nunca foi escrita: clicar nele derrubava o
 * script com ReferenceError e, de quebra, interrompia os cliques seguintes.
 */
function toggleFonteLegivel() {
    var ativo = document.body.classList.toggle('fonte-legivel');
    gravarPreferencia('fonteLegivel', ativo ? 'true' : 'false');
}

/** Comando de voz para buscar um local. Também estava no menu sem implementação. */
function ativarVoz() {
    var Reconhecimento = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!Reconhecimento) {
        MobiliTudoMapa.mostrarAviso('Seu navegador não oferece reconhecimento de voz. '
            + 'No Chrome ou no Edge o recurso funciona.', 'alerta', 'voz');
        return;
    }
    var reconhecimento = new Reconhecimento();
    reconhecimento.lang = 'pt-BR';
    reconhecimento.interimResults = false;
    reconhecimento.maxAlternatives = 1;

    MobiliTudoMapa.mostrarAviso('Ouvindo... diga o nome de um local.', 'info', 'voz');

    reconhecimento.onresult = function (evento) {
        var dito = evento.results[0][0].transcript;
        document.getElementById('searchInput').value = dito;
        MobiliTudoMapa.esconderAviso('voz');
        buscarLocal();
    };
    reconhecimento.onerror = function () {
        MobiliTudoMapa.mostrarAviso('Não consegui entender. Tente novamente.', 'alerta', 'voz');
    };
    reconhecimento.start();
}

function gravarPreferencia(chave, valor) {
    try {
        localStorage.setItem(chave, valor);
    } catch (erro) {
        /* armazenamento indisponível: a preferência vale só nesta sessão */
    }
}

function lerPreferencia(chave) {
    try {
        return localStorage.getItem(chave);
    } catch (erro) {
        return null;
    }
}

// --------------------------------------------------------------------------
// 8. Lightbox
// --------------------------------------------------------------------------

function expandirFoto(imagem) {
    document.getElementById('lightboxImg').src = imagem.src;
    document.getElementById('lightboxOverlay').classList.add('active');
}

function fecharLightbox(evento) {
    if (evento.target.id === 'lightboxOverlay' || evento.target.classList.contains('close-lightbox')) {
        document.getElementById('lightboxOverlay').classList.remove('active');
    }
}

// --------------------------------------------------------------------------
// 9. Inicialização
// --------------------------------------------------------------------------

window.addEventListener('load', function () {
    if (lerPreferencia('altoContraste') === 'true') {
        document.body.classList.add('alto-contraste');
    }
    if (lerPreferencia('fonteLegivel') === 'true') {
        document.body.classList.add('fonte-legivel');
    }

    var nome = lerPreferencia('nomeUsuario');
    var placeholder = document.querySelector('.placeholder-text');
    if (nome && placeholder) {
        placeholder.textContent = 'Olá, ' + nome + '! Toque em um ponto do mapa para ver a acessibilidade.';
    }

    carregarLocais().then(function () {
        MobiliTudoObstaculos.carregar([-23.9925, -46.4095]);
        // A leitura automática só dispara depois que há conteúdo na tela para ler.
        if (lerPreferencia('preferenciaAcessibilidade') === 'visual') {
            ativarLeitor();
        }
    });
});
