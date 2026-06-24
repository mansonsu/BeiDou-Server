package org.gms.idle;

public final class IdleCombatSnapshot {
    private final int gainedExp;
    private final int gainedMeso;
    private final int lastKills;

    public IdleCombatSnapshot(int gainedExp, int gainedMeso, int lastKills) {
        this.gainedExp = gainedExp;
        this.gainedMeso = gainedMeso;
        this.lastKills = lastKills;
    }

    public int getGainedExp() {
        return gainedExp;
    }

    public int getGainedMeso() {
        return gainedMeso;
    }

    public int getLastKills() {
        return lastKills;
    }
}
