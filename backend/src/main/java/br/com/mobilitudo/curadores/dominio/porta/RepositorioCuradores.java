package br.com.mobilitudo.curadores.dominio.porta;

import br.com.mobilitudo.curadores.dominio.modelo.Curador;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioCuradores {

    Curador salvar(Curador curador);

    Optional<Curador> porId(UUID id);

    Optional<Curador> porEmail(String email);

    boolean existeAlgum();

    List<Curador> todos();
}
