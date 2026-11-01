package com.insurtech.kernel.statemachine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.insurtech.kernel.error.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.Test;

class StateMachineTest {

  enum LineState {
    OPEN,
    CLOSED,
    VOIDED
  }

  sealed interface LineCommand {}

  record Close() implements LineCommand {}

  record Reopen() implements LineCommand {}

  record VoidLine() implements LineCommand {}

  record Line(boolean hasOutstandingReserve, boolean hasPendingPayments) {}

  static final ErrorCode OUTSTANDING_RESERVE = ErrorCode.of("OUTSTANDING_RESERVE");
  static final ErrorCode PENDING_PAYMENTS = ErrorCode.of("PENDING_PAYMENTS");

  static final StateMachine<LineState, LineCommand, Line> MACHINE =
      StateMachine.<LineState, LineCommand, Line>builder("ClaimLine")
          .transition(
              LineState.OPEN,
              Close.class,
              LineState.CLOSED,
              List.of(
                  (line, cmd) ->
                      GuardResult.require(!line.hasOutstandingReserve(), OUTSTANDING_RESERVE),
                  (line, cmd) -> GuardResult.require(!line.hasPendingPayments(), PENDING_PAYMENTS)))
          .transition(LineState.CLOSED, Reopen.class, LineState.OPEN)
          .transition(LineState.OPEN, VoidLine.class, LineState.VOIDED)
          .build();

  static final Line CLEAN = new Line(false, false);

  @Test
  void firesDeclaredTransitions() {
    assertThat(MACHINE.fire(LineState.OPEN, new Close(), CLEAN)).isEqualTo(LineState.CLOSED);
    assertThat(MACHINE.fire(LineState.CLOSED, new Reopen(), CLEAN)).isEqualTo(LineState.OPEN);
  }

  @Test
  void rejectsUndeclaredTransitionsLoudly() {
    assertThatThrownBy(() -> MACHINE.fire(LineState.VOIDED, new Reopen(), CLEAN))
        .isInstanceOf(IllegalStateTransitionException.class)
        .hasMessageContaining("VOIDED");
  }

  @Test
  void reportsEveryFailingGuard() {
    assertThatThrownBy(() -> MACHINE.fire(LineState.OPEN, new Close(), new Line(true, true)))
        .isInstanceOfSatisfying(
            GuardFailedException.class,
            e -> assertThat(e.reasons()).containsExactly(OUTSTANDING_RESERVE, PENDING_PAYMENTS));
  }

  @Test
  void everyIllegalPairIsRejected() {
    List<LineCommand> commands = List.of(new Close(), new Reopen(), new VoidLine());
    for (LineState state : LineState.values()) {
      for (LineCommand command : commands) {
        boolean declared = MACHINE.commandsFrom(state).contains(command.getClass());
        if (!declared) {
          assertThatThrownBy(() -> MACHINE.fire(state, command, CLEAN))
              .isInstanceOf(IllegalStateTransitionException.class);
        }
      }
    }
  }

  @Test
  void rejectsDuplicateDeclarations() {
    StateMachine.Builder<LineState, LineCommand, Line> builder =
        StateMachine.<LineState, LineCommand, Line>builder("Dup")
            .transition(LineState.OPEN, Close.class, LineState.CLOSED);
    assertThatThrownBy(() -> builder.transition(LineState.OPEN, Close.class, LineState.VOIDED))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void exportsMermaid() {
    assertThat(MACHINE.toMermaid())
        .startsWith("stateDiagram-v2\n")
        .contains("OPEN --> CLOSED : Close")
        .contains("CLOSED --> OPEN : Reopen");
  }
}
