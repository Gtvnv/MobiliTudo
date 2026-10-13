package br.com.mobilitudo.curadores.infraestrutura.web;

import br.com.mobilitudo.curadores.aplicacao.AutenticarCurador;
import br.com.mobilitudo.curadores.aplicacao.GerenciarCuradores;
import br.com.mobilitudo.curadores.dominio.modelo.Curador;
import br.com.mobilitudo.curadores.dominio.modelo.CredenciaisInvalidasException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/curadores")
@Tag(name = "Curadores", description = "Contas de acesso à curadoria de obstáculos")
class CuradorController {

    private final AutenticarCurador autenticarCurador;
    private final GerenciarCuradores gerenciarCuradores;

    CuradorController(AutenticarCurador autenticarCurador, GerenciarCuradores gerenciarCuradores) {
        this.autenticarCurador = autenticarCurador;
        this.gerenciarCuradores = gerenciarCuradores;
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica um curador por e-mail e senha",
            description = "Responde 401 para e-mail não cadastrado, senha errada ou conta desativada — "
                    + "sempre com a mesma mensagem, para não revelar qual dos três casos é. Responde 429 "
                    + "acima do limite de tentativas por hora da origem.")
    LoginRespostaDTO login(@RequestBody @Valid LoginDTO corpo, HttpServletRequest requisicao) {
        return LoginRespostaDTO.de(autenticarCurador.login(corpo.email(), corpo.senha(), requisicao));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Encerra a sessão do token enviado",
            description = "Idempotente: um token já inválido ou já encerrado não gera erro.")
    void logout(HttpServletRequest requisicao) {
        autenticarCurador.logout(tokenDaRequisicao(requisicao));
    }

    @GetMapping("/me")
    @Operation(summary = "Dados da conta autenticada pelo token enviado")
    CuradorDTO me(HttpServletRequest requisicao) {
        return CuradorDTO.de(autenticarCurador.autenticarPorToken(tokenDaRequisicao(requisicao)));
    }

    @GetMapping
    @Operation(summary = "Lista todas as contas de curador",
            description = "Exige uma sessão de curador válida.")
    List<CuradorDTO> listar(HttpServletRequest requisicao) {
        autenticarCurador.autenticarPorToken(tokenDaRequisicao(requisicao));
        return gerenciarCuradores.todos().stream().map(CuradorDTO::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra uma nova conta de curador",
            description = "Exige uma sessão de curador válida — qualquer curador ativo pode cadastrar outro, "
                    + "não há papel de administrador separado. Responde 409 se o e-mail já estiver cadastrado "
                    + "e 400 para senha com menos de 8 caracteres.")
    CuradorDTO cadastrar(@RequestBody @Valid NovoCuradorDTO corpo, HttpServletRequest requisicao) {
        autenticarCurador.autenticarPorToken(tokenDaRequisicao(requisicao));
        Curador curador = gerenciarCuradores.cadastrar(corpo.email(), corpo.nomeDeExibicao(), corpo.senha());
        return CuradorDTO.de(curador);
    }

    @PostMapping("/{id}/desativar")
    @Operation(summary = "Desativa uma conta de curador",
            description = "Exige uma sessão de curador válida. Sessões já abertas da conta desativada "
                    + "deixam de autenticar na próxima verificação — ver AutenticarCurador.autenticarPorToken.")
    CuradorDTO desativar(@PathVariable UUID id, HttpServletRequest requisicao) {
        autenticarCurador.autenticarPorToken(tokenDaRequisicao(requisicao));
        return CuradorDTO.de(gerenciarCuradores.desativar(id));
    }

    private static String tokenDaRequisicao(HttpServletRequest requisicao) {
        String cabecalho = requisicao.getHeader("Authorization");
        if (cabecalho == null || !cabecalho.startsWith("Bearer ")) {
            throw new CredenciaisInvalidasException();
        }
        return cabecalho.substring("Bearer ".length()).trim();
    }
}
