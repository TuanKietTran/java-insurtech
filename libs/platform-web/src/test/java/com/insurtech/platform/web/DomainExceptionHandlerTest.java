package com.insurtech.platform.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.insurtech.kernel.error.ErrorCode;
import com.insurtech.kernel.statemachine.GuardFailedException;
import com.insurtech.kernel.statemachine.IllegalStateTransitionException;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

class DomainExceptionHandlerTest {

  private final DomainExceptionHandler handler = new DomainExceptionHandler();

  @Test
  void illegalTransitionIsConflictWithStableType() {
    ProblemDetail problem =
        handler.handle(new IllegalStateTransitionException("Claim", "CLOSED", "CloseClaim"));

    assertThat(problem.getStatus()).isEqualTo(409);
    assertThat(problem.getType())
        .isEqualTo(URI.create("urn:insurtech:problem:illegal-state-transition"));
  }

  @Test
  void guardFailureListsEveryReason() {
    ProblemDetail problem =
        handler.handle(
            new GuardFailedException(
                "ClaimLine",
                "OPEN",
                "CloseClaimLine",
                List.of(ErrorCode.of("OUTSTANDING_RESERVE"), ErrorCode.of("PENDING_PAYMENTS"))));

    assertThat(problem.getStatus()).isEqualTo(422);
    assertThat(problem.getProperties())
        .containsEntry("reasons", List.of("OUTSTANDING_RESERVE", "PENDING_PAYMENTS"));
  }
}
