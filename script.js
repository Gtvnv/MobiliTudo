// 1. CONFIGURAÇÃO DO MAPA
let locais = [];
const map = L.map('map', { zoomControl: false }).setView([-24.008, -46.412], 13);

L.control.zoom({ position: 'topright' }).addTo(map);

L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap'
}).addTo(map);

// 2. BANCO DE DADOS (Com categorias e recursos para os ícones circulares)
fetch('locais.json')
    .then(response => {
        if (!response.ok) throw new Error("Erro ao carregar o arquivo JSON");
        return response.json();
    })
    .then(dados => {
        locais = dados; // Salva os dados na nossa variável global
        renderizarMarcadores(locais); // Desenha os pontos no mapa
        console.log("Banco de dados carregado com sucesso!")
    })
    .catch(error => console.error("Erro na carga de dados:", error));

let markers = [];

// 3. FUNÇÕES DE INTERFACE (MENU E DETALHES COM ÍCONES CIRCULARES)

function toggleMenu() {
    const menu = document.getElementById("sideMenu");
    if (menu.style.width === "350px") {
        menu.style.width = "0";
    } else {
        menu.style.width = "350px";
    }
}

function mostrarDetalhes(local) {
    const container = document.getElementById("placeDetails");
    
    // Gera os círculos de recursos (estilo Google Maps)
    const recursosHTML = local.recursos.map(rec => `
        <div class="recurso-item">
            <div class="recurso-circulo" title="${rec}">
                ${obterIconeRecurso(rec)}
            </div>
            <span>${rec}</span>
        </div>
    `).join('');

    container.innerHTML = `
        <div class="place-container">
            <img src="${local.imagem}" class="place-img" onclick="expandirFoto(this)">
            <div class="place-info">
                <h3 id="detalheTitulo">${local.nome}</h3>
                
                <div class="recursos-wrapper">
                    ${recursosHTML}
                </div>

                <p id="detalheTexto">${local.info}</p>
                <hr>
                <button class="menu-item" onclick="window.open('https://www.google.com/maps/dir/?api=1&destination=${local.coords[0]},${local.coords[1]}')">
                    📍 Como chegar
                </button>
            </div>
        </div>
    `;
    document.getElementById("sideMenu").style.width = "350px";
}

function obterIconeRecurso(recurso) {
    const icones = {
        "Rampas": "♿",
        "Piso Tátil": "👣",
        "Elevador": "🛗",
        "Placas Braille": "⠃"
    };
    return icones[recurso] || "✅";
}

// 4. LÓGICA DE BUSCA E FILTROS

function renderizarMarcadores(lista) {
    markers.forEach(m => map.removeLayer(m));
    markers = [];
    lista.forEach(local => {
        const m = L.marker(local.coords).addTo(map);
        m.on('click', () => mostrarDetalhes(local));
        markers.push(m);
    });
}

function buscarLocal() {
    const busca = document.getElementById("searchInput").value.toLowerCase();
    const achado = locais.find(l => l.nome.toLowerCase().includes(busca));
    if (achado) {
        map.flyTo(achado.coords, 16);
        mostrarDetalhes(achado);
    } else {
        alert("Local não encontrado.");
    }
}

function toggleFiltros() {
    document.getElementById("filterBar").classList.toggle("active");
}

function filtrarPor(categoria) {
    const filtrados = (categoria === 'todos') ? locais : locais.filter(l => l.categoria === categoria);
    renderizarMarcadores(filtrados);
}

// 5. GEOLOCALIZAÇÃO REAL

function minhaLocalizacao() {
    if (!navigator.geolocation) {
        alert("Seu navegador não suporta geolocalização.");
        return;
    }

    const btn = document.getElementById('btn-geoloc');
    btn.innerText = "⏳";

    navigator.geolocation.getCurrentPosition(
        (posicao) => {
            const lat = posicao.coords.latitude;
            const lng = posicao.coords.longitude;
            map.flyTo([lat, lng], 16);

            L.circleMarker([lat, lng], {
                radius: 8, fillColor: "#4285F4", color: "#fff", weight: 2, opacity: 1, fillOpacity: 0.8
            }).addTo(map).bindPopup("Você está aqui!").openPopup();

            btn.innerText = "🎯";
        },
        () => {
            alert("Erro ao obter localização. Verifique as permissões do seu navegador.");
            btn.innerText = "🎯";
        },
        { enableHighAccuracy: true }
    );
}

// 6. ACESSIBILIDADE E NAVEGAÇÃO

function toggleSubMenu(id) {
    const sub = document.getElementById(id);
    sub.style.display = (sub.style.display === "block") ? "none" : "block";
}

function toggleContraste() {
    document.body.classList.toggle("alto-contraste");
}

function ativarLeitor() {
    const titulo = document.getElementById("detalheTitulo")?.innerText || "Mobili Tudo";
    const texto = document.getElementById("detalheTexto")?.innerText || "Selecione um local.";
    const msg = new SpeechSynthesisUtterance(`${titulo}. ${texto}`);
    msg.lang = 'pt-BR';
    window.speechSynthesis.speak(msg);
}

// 7. LIGHTBOX E INICIALIZAÇÃO

function expandirFoto(imgElement) {
    const overlay = document.getElementById("lightboxOverlay");
    const lightboxImg = document.getElementById("lightboxImg");
    lightboxImg.src = imgElement.src;
    overlay.classList.add("active");
}

function fecharLightbox(event) {
    const overlay = document.getElementById("lightboxOverlay");
    if (event.target.id === "lightboxOverlay" || event.target.classList.contains("close-lightbox")) {
        overlay.classList.remove("active");
    }
}

// Inicializa os pontos no mapa e verifica preferências salvas
renderizarMarcadores(locais);

window.addEventListener('load', () => {
    if (localStorage.getItem("preferenciaAcessibilidade") === "visual") ativarLeitor();
    if (localStorage.getItem("altoContraste") === "true") document.body.classList.add("alto-contraste");

window.addEventListener('load', () => {
    // 1. Verifica se o usuário está logado para mudar o texto da sidebar
    const nomeSalvo = localStorage.getItem("nomeUsuario");
    const placeholderTexto = document.querySelector(".placeholder-text");
    
    if (nomeSalvo && placeholderTexto) {
        placeholderTexto.innerText = `Olá, ${nomeSalvo}! Selecione um ponto no mapa para ver a acessibilidade.`;
    }

    // 2. Preferências de Acessibilidade (o que já tínhamos feito)
    if (localStorage.getItem("preferenciaAcessibilidade") === "visual") ativarLeitor();
    if (localStorage.getItem("altoContraste") === "true") document.body.classList.add("alto-contraste");
});
});