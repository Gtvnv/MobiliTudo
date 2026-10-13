package br.com.mobilitudo.curadores.aplicacao;

import br.com.mobilitudo.curadores.dominio.modelo.Curador;
import br.com.mobilitudo.curadores.dominio.modelo.CuradorNaoEncontradoException;
import br.com.mobilitudo.curadores.dominio.modelo.EmailJaCadastradoException;
import br.com.mobilitudo.curadores.dominio.porta.RepositorioCuradores;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Cadastro e desativação de contas de curador.
 *
 * <p>Qualquer curador ativo pode cadastrar ou desativar outro — sem hierarquia de
 * administrador separada, ver o porquê em {@link Curador}.</p>
 */
@Service
public class GerenciarCuradores {

    private final RepositorioCuradores repositorio;

    public GerenciarCuradores(RepositorioCuradores repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public Curador cadastrar(String email, String nomeDeExibicao, String senha) {
        String emailNormalizado = email == null ? "" : email.trim().toLowerCase();
        if (repositorio.porEmail(emailNormalizado).isPresent()) {
            throw new EmailJaCadastradoException(emailNormalizado);
        }
        return repositorio.salvar(Curador.criar(emailNormalizado, nomeDeExibicao, senha));
    }

    @Transactional
    public Curador desativar(UUID id) {
        Curador curador = repositorio.porId(id).orElseThrow(() -> new CuradorNaoEncontradoException(id));
        curador.desativar();
        return repositorio.salvar(curador);
    }

    @Transactional(readOnly = true)
    public List<Curador> todos() {
        return repositorio.todos();
    }

    /** Para o bootstrap do primeiro curador — ver {@code BootstrapDoPrimeiroCurador}. */
    @Transactional(readOnly = true)
    public boolean existeAlgumCurador() {
        return repositorio.existeAlgum();
    }
}
