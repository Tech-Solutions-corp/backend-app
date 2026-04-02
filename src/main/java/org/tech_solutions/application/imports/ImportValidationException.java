package org.tech_solutions.application.imports;

public class ImportValidationException extends RuntimeException {
    public ImportValidationException(String message) {
        super(message);
    }

    public ImportValidationException() {
        super("Arquivo inválido");
    }
}
