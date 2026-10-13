package org.polydevs.opusmc.optim.core.workload;

/** Uses the caller's signed nanoTime subtraction convention; zero means single-task drain. */
public final class DeadlineBudget {
    private DeadlineBudget() { }
    public static long cap(long deadline, long now, long budget) {
        if (budget <= 0 || budget > 1_000_000_000L) { throw new IllegalArgumentException("Invalid work budget"); }
        if (deadline == 0 || deadline - now <= budget) { return deadline; }
        return now + budget;
    }
}
