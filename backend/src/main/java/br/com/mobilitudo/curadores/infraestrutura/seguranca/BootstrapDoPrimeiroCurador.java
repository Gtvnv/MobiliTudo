package br.com.mobilitudo.curadores.infraestrutura.seguranca;

import br.com.mobilitudo.curadores.aplicacao.GerenciarCuradores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Cria o primeiro curador a partir de variáveis de ambiente, quando a tabela de
 * curadores está vazia — o mesmo problema do ovo e da galinha que
 * {@code MOBILITUDO_CURADOR_TOKEN} resolvia antes de existir conta individual: sem uma
 * conta já cadastrada, ninguém consegue logar para cadastrar a primeira.
 *
 * <p>Nunca escreve uma credencial padrão no código nem numa migração — isso seria
 * exatamente o tipo de segredo-commitado-no-git que o projeto já evita em
 * {@code MOBILITUDO_CURADOR_TOKEN} (ver {@code docker-compose.yml}). Sem as duas
 * variáveis definidas, simplesmente não cria ninguém: a curadoria fica inacessível até
 * alguém configurar o bootstrap, falha fechada como o mecanismo anterior.</p>
 *
 * <p>Roda só quando a tabela está vazia, então definir as variáveis depois do primeiro
 * curador já existir não cria uma segunda conta nem sobrescreve nada — é seguro deixá-las
 * configuradas permanentemente no ambiente.</p>
 */
@Component
class BootstrapDoPrimeiroCurador implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(BootstrapDoPrimeiroCurador.class);

    private final GerenciarCuradores gerenciarCuradores;
    private final String emailInicial;
    private final String senhaInicial;

    BootstrapDoPrimeiroCurador(GerenciarCuradores gerenciarCuradores,
                               @Value("${mobilitudo.seguranca.curador-inicial-email:}") String emailInicial,
                               @Value("${mobilitudo.seguranca.curador-inicial-senha:}") String senhaInicial) {
        this.gerenciarCuradores = gerenciarCuradores;
        this.emailInicial = emailInicial;
        this.senhaInicial = senhaInicial;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (gerenciarCuradores.existeAlgumCurador()) {
            return;
        }
        if (emailInicial.isBlank() || senhaInicial.isBlank()) {
            LOG.warn("Nenhum curador cadastrado e MOBILITUDO_CURADOR_INICIAL_EMAIL/SENHA não definidos — "
                    + "a curadoria de obstáculos ficará inacessível até alguém configurar o bootstrap.");
            return;
        }
        gerenciarCuradores.cadastrar(emailInicial, "Curador inicial", senhaInicial);
        LOG.info("Primeiro curador criado a partir de MOBILITUDO_CURADOR_INICIAL_EMAIL.");
    }
}
