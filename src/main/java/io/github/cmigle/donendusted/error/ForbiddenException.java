package io.github.cmigle.donendusted.error;

/** 403 - naudotojas zinomas, bet neturi teises atlikti sio veiksmo. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
