package org.gms.idle;

public final class IdleCombatResult {
    private final int elapsedSeconds;
    private final int kills;
    private final int exp;
    private final int meso;

    public IdleCombatResult(int elapsedSeconds, int kills, int exp, int meso) {
        this.elapsedSeconds = elapsedSeconds;
        this.kills = kills;
        this.exp = exp;
        this.meso = meso;
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

    public int getMeso() {
        return meso;
    }

}
