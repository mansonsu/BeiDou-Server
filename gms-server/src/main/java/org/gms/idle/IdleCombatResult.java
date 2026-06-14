package org.gms.idle;

public final class IdleCombatResult {
    private final int elapsedSeconds;
    private final int kills;
    private final int exp;
    private final int meso;
    private final int commonDrops;
    private final int rareDrops;

    public IdleCombatResult(int elapsedSeconds, int kills, int exp, int meso, int commonDrops, int rareDrops) {
        this.elapsedSeconds = elapsedSeconds;
        this.kills = kills;
        this.exp = exp;
        this.meso = meso;
        this.commonDrops = commonDrops;
        this.rareDrops = rareDrops;
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

    public int getCommonDrops() {
        return commonDrops;
    }

    public int getRareDrops() {
        return rareDrops;
    }
}
