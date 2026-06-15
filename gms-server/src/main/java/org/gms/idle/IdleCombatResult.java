package org.gms.idle;

public final class IdleCombatResult {
    private final int elapsedSeconds;
    private final int kills;
    private final double remainingDamage;
    private final int estimatedDamagePerAttack;
    private final int monsterId;

    public IdleCombatResult(int elapsedSeconds, int kills) {
        this(elapsedSeconds, kills, 0.0D, 0, 0);
    }

    public IdleCombatResult(int elapsedSeconds, int kills, double remainingDamage) {
        this(elapsedSeconds, kills, remainingDamage, 0, 0);
    }

    public IdleCombatResult(int elapsedSeconds, int kills, double remainingDamage, int estimatedDamagePerAttack, int monsterId) {
        this.elapsedSeconds = elapsedSeconds;
        this.kills = kills;
        this.remainingDamage = remainingDamage;
        this.estimatedDamagePerAttack = estimatedDamagePerAttack;
        this.monsterId = monsterId;
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

    public int getEstimatedDamagePerAttack() {
        return estimatedDamagePerAttack;
    }

    public int getMonsterId() {
        return monsterId;
    }
}
