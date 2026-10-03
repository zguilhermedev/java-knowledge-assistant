package br.com.guilherme.knowledgeassistant.exception;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(KnowledgeException.class)
    ProblemDetail handleKnowledgeException(KnowledgeException exception) {
        return ProblemDetail.forStatusAndDetail(exception.status(), exception.getMessage());
    }
}
