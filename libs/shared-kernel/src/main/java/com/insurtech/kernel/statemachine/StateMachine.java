package com.insurtech.kernel.statemachine;

import com.insurtech.kernel.error.ErrorCode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Declarative transition table for a lifecycle aggregate (CRC-SM-001). Every legal {@code (state,
 * command)} pair is declared once with its guards and target state; anything else is rejected with
 * {@link IllegalStateTransitionException} (CRC-SM-004). All failing guards are reported together
 * (CRC-SM-003). {@link #toMermaid()} exports the table for generated documentation (CRC-SM-005).
 *
 * @param <S> state enum
 * @param <C> command base type
 * @param <X> guard subject, usually the aggregate
 */
public final class StateMachine<S extends Enum<S>, C, X> {

  /** One legal transition. */
  public record Transition<S extends Enum<S>, C, X>(
      S from, Class<? extends C> command, S to, List<Guard<X, C>> guards) {
    public Transition {
      Objects.requireNonNull(from, "from");
      Objects.requireNonNull(command, "command");
      Objects.requireNonNull(to, "to");
      guards = List.copyOf(guards);
    }
  }

  private final String name;
  private final List<Transition<S, C, X>> transitions;

  private StateMachine(String name, List<Transition<S, C, X>> transitions) {
    this.name = name;
    this.transitions = List.copyOf(transitions);
  }

  public static <S extends Enum<S>, C, X> Builder<S, C, X> builder(String name) {
    return new Builder<>(name);
  }

  /**
   * Returns the target state for {@code command} in {@code current}, after all guards pass.
   *
   * @throws IllegalStateTransitionException if the transition is not declared
   * @throws GuardFailedException if any guard fails, listing every failing reason
   */
  public S fire(S current, C command, X subject) {
    Transition<S, C, X> transition =
        find(current, command)
            .orElseThrow(
                () ->
                    new IllegalStateTransitionException(
                        name, current.name(), command.getClass().getSimpleName()));
    List<ErrorCode> failures = new ArrayList<>();
    for (Guard<X, C> guard : transition.guards()) {
      if (guard.check(subject, command) instanceof GuardResult.Fail fail) {
        failures.addAll(fail.reasons());
      }
    }
    if (!failures.isEmpty()) {
      throw new GuardFailedException(
          name, current.name(), command.getClass().getSimpleName(), failures);
    }
    return transition.to();
  }

  /** Commands declared for {@code current}, ignoring guards; used for "allowed actions" APIs. */
  public Set<Class<? extends C>> commandsFrom(S current) {
    Set<Class<? extends C>> commands = new LinkedHashSet<>();
    for (Transition<S, C, X> transition : transitions) {
      if (transition.from() == current) {
        commands.add(transition.command());
      }
    }
    return Set.copyOf(commands);
  }

  public List<Transition<S, C, X>> transitions() {
    return transitions;
  }

  public String name() {
    return name;
  }

  /** Mermaid {@code stateDiagram-v2} rendering of the transition table. */
  public String toMermaid() {
    StringBuilder diagram = new StringBuilder("stateDiagram-v2\n");
    for (Transition<S, C, X> transition : transitions) {
      diagram
          .append("  ")
          .append(transition.from().name())
          .append(" --> ")
          .append(transition.to().name())
          .append(" : ")
          .append(transition.command().getSimpleName())
          .append('\n');
    }
    return diagram.toString();
  }

  private Optional<Transition<S, C, X>> find(S current, C command) {
    for (Transition<S, C, X> transition : transitions) {
      if (transition.from() == current && transition.command().isInstance(command)) {
        return Optional.of(transition);
      }
    }
    return Optional.empty();
  }

  /** Builder that rejects duplicate {@code (from, command)} declarations. */
  public static final class Builder<S extends Enum<S>, C, X> {

    private final String name;
    private final List<Transition<S, C, X>> transitions = new ArrayList<>();

    private Builder(String name) {
      this.name = Objects.requireNonNull(name, "name");
    }

    public Builder<S, C, X> transition(S from, Class<? extends C> command, S to) {
      return transition(from, command, to, List.of());
    }

    public Builder<S, C, X> transition(
        S from, Class<? extends C> command, S to, List<Guard<X, C>> guards) {
      for (Transition<S, C, X> existing : transitions) {
        if (existing.from() == from && existing.command().equals(command)) {
          throw new IllegalStateException(
              name + ": duplicate transition " + from + " + " + command.getSimpleName());
        }
      }
      transitions.add(new Transition<>(from, command, to, guards));
      return this;
    }

    public StateMachine<S, C, X> build() {
      if (transitions.isEmpty()) {
        throw new IllegalStateException(name + ": a state machine needs at least one transition");
      }
      return new StateMachine<>(name, transitions);
    }
  }
}
