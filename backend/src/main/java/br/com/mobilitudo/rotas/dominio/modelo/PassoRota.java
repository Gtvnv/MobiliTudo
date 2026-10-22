package br.com.mobilitudo.rotas.dominio.modelo;

import br.com.mobilitudo.compartilhado.dominio.NivelAcessibilidade;

import java.util.List;

/**
 * Uma instrucao de navegacao, correspondente a um trecho percorrido.
 *
 * @param instrucao      texto pronto para leitura por sintetizador de voz
 * @param nomeVia        logradouro do trecho
 * @param segmentoId     identificador do trecho na malha
 * @param distanciaMetros extensao do trecho
 * @param nivel          como este trecho se classifica para o perfil pedido
 * @param ressalvas      pontos de atencao do trecho, vazio quando ele e plenamente acessivel
 */
public record PassoRota(String instrucao,
                        String nomeVia,
                        String segmentoId,
                        double distanciaMetros,
                        NivelAcessibilidade nivel,
                        List<String> ressalvas) {

    public PassoRota {
        ressalvas = ressalvas == null ? List.of() : List.copyOf(ressalvas);
    }
}
