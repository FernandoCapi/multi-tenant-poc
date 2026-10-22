package com.vesta.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.vesta.auth.InvalidCredentialsException;
import com.vesta.user.UnknownAuthenticatedUserException;

/** Maps errors to problem details with Portuguese messages (docs/requisitos.md §7). */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail handleInvalidCredentials() {
        // Same answer for unknown e-mail and wrong password (CA-05.2)
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");
        problem.setTitle("Não autenticado");
        return problem;
    }

    @ExceptionHandler(UnknownAuthenticatedUserException.class)
    ProblemDetail handleUnknownAuthenticatedUser() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Faça login para acessar este recurso.");
        problem.setTitle("Não autenticado");
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_CONTENT, "Verifique os campos obrigatórios e tente novamente.");
        problem.setTitle("Dados inválidos");
        return ResponseEntity.unprocessableContent().body(problem);
    }
}
