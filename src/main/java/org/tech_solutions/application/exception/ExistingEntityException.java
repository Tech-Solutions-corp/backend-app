package org.tech_solutions.application.exception;

public class ExistingEntityException extends RuntimeException {
    public ExistingEntityException() {
        super("Entidade ja existente no sistema");
    }

    public ExistingEntityException(String message) {
        super(message);
    }
}
