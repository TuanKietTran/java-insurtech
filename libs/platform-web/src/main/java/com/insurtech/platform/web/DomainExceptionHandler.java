package com.insurtech.platform.web;

import com.insurtech.kernel.error.DomainException;
import com.insurtech.kernel.error.ErrorCode;
import com.insurtech.kernel.error.InvalidValueException;
import com.insurtech.kernel.statemachine.GuardFailedException;
import com.insurtech.kernel.statemachine.IllegalStateTransitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps domain exceptions to RFC 9457 Problem Details at the edge (CRC-ERR-001, CRC-API-003). */
@RestControllerAdvice
public class DomainExceptionHandler {

  @ExceptionHandler(DomainException.class)
  public ProblemDetail handle(DomainException exception) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(statusOf(exception), exception.getMessage());
    problem.setType(Problems.typeFor(exception.code().value()));
    problem.setProperty("code", exception.code().value());
    if (exception instanceof GuardFailedException guardFailed) {
      problem.setProperty("reasons", guardFailed.reasons().stream().map(ErrorCode::value).toList());
    }
    return problem;
  }

  static HttpStatus statusOf(DomainException exception) {
    if (exception instanceof IllegalStateTransitionException) {
      return HttpStatus.CONFLICT;
    }
    if (exception instanceof InvalidValueException) {
      return HttpStatus.BAD_REQUEST;
    }
    return HttpStatus.UNPROCESSABLE_CONTENT;
  }
}
