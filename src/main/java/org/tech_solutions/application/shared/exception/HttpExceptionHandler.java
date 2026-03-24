package org.tech_solutions.application.shared.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.tech_solutions.application.shared.dto.ErrorDTO;

@RestControllerAdvice
public class HttpExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpExceptionHandler.class);

    @ExceptionHandler(ExistingEntityException.class)
    public ResponseEntity<ErrorDTO> entidadeExistenteHandler(ExistingEntityException ex) {
        var response = new ErrorDTO(
                HttpStatus.CONFLICT.value(),
                ex.getMessage()
        );
        return ResponseEntity.status(409).body(response);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorDTO> entidadeNaoEncontradaHandler(EntityNotFoundException ex) {
        var response = new ErrorDTO(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDTO> handleValidationError(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Dados invalidos")
                .orElse("Dados invalidos");

        ErrorDTO error = new ErrorDTO(HttpStatus.BAD_REQUEST.value(), message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorDTO> handleBadRequest(IllegalArgumentException ex) {
                ErrorDTO error = new ErrorDTO(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorDTO> handleNotFound(NoHandlerFoundException ex) {
        ErrorDTO error = new ErrorDTO(
                HttpStatus.NOT_FOUND.value(),
                "Endpoint não encontrado: " + ex.getRequestURL()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorDTO> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex) {

        String supportedMethods = ex.getSupportedMethods() == null
                ? "nenhum"
                : String.join(", ", ex.getSupportedMethods());

        String message = String.format(
                "Método %s não suportado para este endpoint. Métodos permitidos: %s",
                ex.getMethod(),
                supportedMethods
        );

        ErrorDTO error = new ErrorDTO(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                message
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDTO> handleGenericException(Exception ex) {
        LOGGER.error("Erro: {}", ex.getMessage());
        ErrorDTO error = new ErrorDTO(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Erro interno no servidor, tente novamente mais tarde"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}