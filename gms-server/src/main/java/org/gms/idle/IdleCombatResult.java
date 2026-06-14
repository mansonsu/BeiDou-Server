package org.gms.idle;

public final class IdleCombatResult {
    private final int elapsedSeconds;
    private final int kills;

    public IdleCombatResult(int elapsedSeconds, int kills) {
        this.elapsedSeconds = elapsedSeconds;
        this.kills = kills;
    }

    public int getElapsedSeconds() {
        return elapsedSeconds;
    }

    public int getKills() {
        return kills;
    }

}
