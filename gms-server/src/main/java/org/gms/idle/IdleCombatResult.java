package org.gms.idle;

public final class IdleCombatResult {
    private final int elapsedSeconds;
    private final int kills;
    private final int exp;

    public IdleCombatResult(int elapsedSeconds, int kills, int exp) {
        this.elapsedSeconds = elapsedSeconds;
        this.kills = kills;
        this.exp = exp;
    }

    public int getElapsedSeconds() {
        return elapsedSeconds;
    }

    public int getKills() {
        return kills;
    }

    public int getExp() {
        return exp;
    }

}
