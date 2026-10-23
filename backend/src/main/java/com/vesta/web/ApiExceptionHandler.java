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

import jakarta.validation.ConstraintViolationException;

import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.vesta.auth.InvalidCredentialsException;
import com.vesta.family.FamilyMemberNotFoundException;
import com.vesta.goal.GoalNotFoundException;
import com.vesta.goal.GoalPermissionDeniedException;
import com.vesta.goal.GoalStateConflictException;
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

    /** Missing, other family or not linked: the same 404, never revealing which (D-09). */
    @ExceptionHandler(GoalNotFoundException.class)
    ProblemDetail handleGoalNotFound() {
        return problem(HttpStatus.NOT_FOUND, "Não encontrado", "Objetivo não encontrado.");
    }

    @ExceptionHandler(FamilyMemberNotFoundException.class)
    ProblemDetail handleFamilyMemberNotFound() {
        return problem(HttpStatus.NOT_FOUND, "Não encontrado", "Membro da família não encontrado.");
    }

    @ExceptionHandler(GoalPermissionDeniedException.class)
    ProblemDetail handleGoalPermissionDenied() {
        return problem(HttpStatus.FORBIDDEN, "Sem permissão", "Você não tem permissão para esta ação.");
    }

    @ExceptionHandler(GoalStateConflictException.class)
    ProblemDetail handleGoalStateConflict(GoalStateConflictException ex) {
        return problem(HttpStatus.CONFLICT, "Ação não permitida", ex.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail handleConcurrentChange() {
        return problem(HttpStatus.CONFLICT, "Alteração simultânea",
                "O objetivo foi alterado por outra pessoa. Recarregue e tente novamente.");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation() {
        return invalidData();
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }

    private static ProblemDetail invalidData() {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Dados inválidos",
                "Verifique os campos obrigatórios e tente novamente.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.unprocessableContent().body(invalidData());
    }
}
