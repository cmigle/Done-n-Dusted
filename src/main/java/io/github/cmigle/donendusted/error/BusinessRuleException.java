package io.github.cmigle.donendusted.error;

/**
 * 422 - JSON suprastas, laukai teisingu tipu, bet turinys pazeidzia dalykines srities taisykle
 * (pvz., GRIND uzduotis be targetCount).
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
