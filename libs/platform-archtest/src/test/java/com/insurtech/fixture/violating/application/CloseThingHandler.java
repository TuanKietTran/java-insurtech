package com.insurtech.fixture.violating.application;

/** Fixture: violates CRC-SEC-001 (handler without @Authorized). */
public final class CloseThingHandler {

  public String handle(String command) {
    return command;
  }
}
