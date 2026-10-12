/**
 * Lembra, só neste navegador, quais obstáculos esta pessoa relatou — e o token de
 * exclusão de cada um —, para que ela consiga remover o próprio nome depois sem
 * precisar copiar e guardar o token em lugar nenhum.
 *
 * Fica em localStorage, não sessionStorage: ao contrário do token de curadoria (um
 * segredo de acesso a uma função administrativa), isto é conveniência pessoal — faz
 * sentido sobreviver ao fechar a aba, para "lembrar depois de uma semana que relatei
 * aquele buraco" continuar funcionando.
 *
 * O servidor nunca vê esta lista: ela não é enviada em requisição alguma, só guarda
 * localmente o que a própria resposta da API já devolveu no momento do relato.
 */
(function (global) {
    'use strict';

    var CHAVE = 'mobilitudo.meusRelatos';
    var LIMITE_DE_ITENS = 200;

    function ler() {
        try {
            var bruto = global.localStorage.getItem(CHAVE);
            return bruto ? JSON.parse(bruto) : [];
        } catch (erro) {
            // JSON corrompido ou armazenamento bloqueado: trata como lista vazia em
            // vez de quebrar a página por causa de uma conveniência.
            return [];
        }
    }

    function gravar(lista) {
        try {
            global.localStorage.setItem(CHAVE, JSON.stringify(lista));
        } catch (erro) {
            /* armazenamento indisponível: a lista vale só para esta sessão de memória */
        }
    }

    /** Registra um relato recém-criado, com o token de exclusão devolvido pela API. */
    function adicionar(obstaculo, tokenExclusao) {
        if (!obstaculo || !obstaculo.id || !tokenExclusao) {
            return;
        }
        var lista = ler();
        lista.unshift({
            id: obstaculo.id,
            tokenExclusao: tokenExclusao,
            tipoDescricao: obstaculo.tipoDescricao,
            coords: obstaculo.coords,
            criadoEm: obstaculo.reportadoEm || new Date().toISOString()
        });
        // Limita o tamanho para não crescer sem fim em localStorage num navegador
        // usado por muito tempo; os mais antigos saem primeiro.
        gravar(lista.slice(0, LIMITE_DE_ITENS));
    }

    function listar() {
        return ler();
    }

    /** Chamado depois que um token foi usado (com sucesso ou não) — ele é de uso único. */
    function remover(obstaculoId) {
        gravar(ler().filter(function (item) {
            return item.id !== obstaculoId;
        }));
    }

    global.MobiliTudoMeusRelatos = {
        adicionar: adicionar,
        listar: listar,
        remover: remover
    };
}(window));
