package io.github.cmigle.donendusted.error;

/** 401 - naudotojas neatpazintas: nera zetono, jis neteisingas, pasibaiges arba blogi prisijungimo duomenys. */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
