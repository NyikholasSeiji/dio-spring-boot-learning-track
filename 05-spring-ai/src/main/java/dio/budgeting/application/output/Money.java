package dio.budgeting.application.output;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class Money {
    private Money() {
    }

    static double centsToReais(long cents) {
        return BigDecimal.valueOf(cents).movePointLeft(2).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
