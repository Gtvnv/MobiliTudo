package br.com.mobilitudo.acessibilidade.dominio;

import br.com.mobilitudo.acessibilidade.dominio.modelo.CategoriaLocal;
import br.com.mobilitudo.acessibilidade.dominio.modelo.Local;
import br.com.mobilitudo.acessibilidade.dominio.modelo.RecursoAcessibilidade;
import br.com.mobilitudo.compartilhado.dominio.Coordenada;
import br.com.mobilitudo.compartilhado.dominio.NivelAcessibilidade;
import br.com.mobilitudo.compartilhado.dominio.PerfilMobilidade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalTest {

    @Test
    @DisplayName("O mesmo local tem niveis diferentes conforme quem pergunta")
    void nivelDependeDoPerfil() {
        Local palacioDasArtes = local(Set.of(
                RecursoAcessibilidade.RAMPA,
                RecursoAcessibilidade.ELEVADOR,
                RecursoAcessibilidade.PLACA_BRAILLE));

        // Entra de cadeira de rodas, mas nao ha banheiro adaptado confirmado.
        assertThat(palacioDasArtes.nivelPara(PerfilMobilidade.CADEIRA_DE_RODAS))
                .isEqualTo(NivelAcessibilidade.COM_RESSALVAS);
        // Braille sem piso tatil nao resolve o deslocamento de quem nao enxerga.
        assertThat(palacioDasArtes.nivelPara(PerfilMobilidade.DEFICIENCIA_VISUAL))
                .isEqualTo(NivelAcessibilidade.INACESSIVEL);
    }

    @Test
    @DisplayName("Sem rampa nem elevador, o local e inacessivel para a cadeira de rodas")
    void semAcessoVerticalEInacessivelParaCadeiraDeRodas() {
        Local semAcesso = local(Set.of(RecursoAcessibilidade.PISO_TATIL));

        assertThat(semAcesso.nivelPara(PerfilMobilidade.CADEIRA_DE_RODAS))
                .isEqualTo(NivelAcessibilidade.INACESSIVEL);
        assertThat(semAcesso.nivelPara(PerfilMobilidade.DEFICIENCIA_VISUAL))
                .isEqualTo(NivelAcessibilidade.COM_RESSALVAS);
    }

    @Test
    @DisplayName("Acesso vertical mais banheiro adaptado fecham o nivel pleno na cadeira de rodas")
    void nivelPlenoParaCadeiraDeRodas() {
        Local completo = local(Set.of(
                RecursoAcessibilidade.RAMPA,
                RecursoAcessibilidade.ELEVADOR,
                RecursoAcessibilidade.BANHEIRO_ADAPTADO,
                RecursoAcessibilidade.PISO_TATIL,
                RecursoAcessibilidade.PLACA_BRAILLE));

        assertThat(completo.nivelPara(PerfilMobilidade.CADEIRA_DE_RODAS))
                .isEqualTo(NivelAcessibilidade.PLENO);
        assertThat(completo.nivelPara(PerfilMobilidade.DEFICIENCIA_VISUAL))
                .isEqualTo(NivelAcessibilidade.PLENO);
    }

    @Test
    @DisplayName("Local sem nome nao e aceito")
    void exigeNome() {
        assertThatThrownBy(() -> new Local(null, "  ", CategoriaLocal.CULTURA,
                new Coordenada(-23.992, -46.411), null, null, null, Set.of(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Os identificadores de categoria usados pelo mapa continuam reconhecidos")
    void interpretaIdentificadoresDoFrontEnd() {
        assertThat(CategoriaLocal.porIdentificador("transporte")).contains(CategoriaLocal.TRANSPORTE);
        assertThat(CategoriaLocal.porIdentificador("CULTURA")).contains(CategoriaLocal.CULTURA);
        assertThat(CategoriaLocal.porIdentificador("inexistente")).isEmpty();
    }

    @Test
    @DisplayName("Os rotulos de recurso ja usados na ficha do local continuam reconhecidos")
    void interpretaRotulosDoFrontEnd() {
        assertThat(RecursoAcessibilidade.porRotuloOuNome("Piso Tátil"))
                .contains(RecursoAcessibilidade.PISO_TATIL);
        assertThat(RecursoAcessibilidade.porRotuloOuNome("RAMPA"))
                .contains(RecursoAcessibilidade.RAMPA);
        assertThat(RecursoAcessibilidade.porRotuloOuNome("Tapete voador")).isEmpty();
    }

    private static Local local(Set<RecursoAcessibilidade> recursos) {
        return new Local(null, "Palácio das Artes", CategoriaLocal.CULTURA,
                new Coordenada(-23.992, -46.411), null, null, null, recursos, null);
    }
}
