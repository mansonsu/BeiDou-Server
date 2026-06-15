package org.gms.idle;

public final class IdleCombatResult {
    private final int elapsedSeconds;
    private final int kills;
    private final double remainingDamage;

    public IdleCombatResult(int elapsedSeconds, int kills) {
        this(elapsedSeconds, kills, 0.0D);
    }

    public IdleCombatResult(int elapsedSeconds, int kills, double remainingDamage) {
        this.elapsedSeconds = elapsedSeconds;
        this.kills = kills;
        this.remainingDamage = remainingDamage;
    }

    public int getElapsedSeconds() {
        return elapsedSeconds;
    }

    public int getKills() {
        return kills;
    }

    public double getRemainingDamage() {
        return remainingDamage;
    }
}
