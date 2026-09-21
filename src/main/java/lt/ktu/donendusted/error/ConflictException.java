package lt.ktu.donendusted.error;

/** 409 - uzklausa teisinga, bet priestarauja esamai sistemos busenai (pvz., vardas uzimtas). */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
