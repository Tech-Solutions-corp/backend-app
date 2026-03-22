package org.tech_solutions.application.shared.exception;

public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException() {
        super("Entidade nao encontrada");
    }

    public EntityNotFoundException(String message) {
        super(message);
    }
}

