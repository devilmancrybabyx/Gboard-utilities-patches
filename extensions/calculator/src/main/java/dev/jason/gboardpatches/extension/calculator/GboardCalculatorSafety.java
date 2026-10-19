package dev.jason.gboardpatches.extension.calculator;

/** Fail-closed execution for calculator lifecycle and view callback boundaries. */
final class GboardCalculatorSafety {
    @FunctionalInterface
    interface BooleanSupplier {
        boolean getAsBoolean();
    }

    private GboardCalculatorSafety() {
    }

    static void run(Runnable action) {
        try {
            action.run();
        } catch (Throwable ignored) {
            // Calculator callbacks must never escape into Gboard.
        }
    }

    static boolean booleanValue(BooleanSupplier action, boolean fallback) {
        try {
            return action.getAsBoolean();
        } catch (Throwable ignored) {
            return fallback;
        }
    }
}
