package br.com.mobilitudo.compartilhado.infraestrutura.seguranca;

/** A curadoria não tem {@code MOBILITUDO_CURADOR_TOKEN} configurado no servidor. */
public class CuradoriaIndisponivelException extends RuntimeException {

    public CuradoriaIndisponivelException() {
        super("Curadoria de obstáculos desativada: defina MOBILITUDO_CURADOR_TOKEN no servidor.");
    }
}
