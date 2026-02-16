package org.tech_solutions.application.exception;

public class EntidadeExistenteException extends RuntimeException {
    public EntidadeExistenteException() {
        super("Entidade ja existente no sistema");
    }

    public EntidadeExistenteException(String message) {
        super(message);
    }
}
