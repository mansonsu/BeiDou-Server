package org.gms.idle;

public final class IdleCombatSnapshot {
    private final int characterId;
    private final int stageId;
    private final String stageName;
    private final int elapsedSeconds;
    private final int totalKills;
    private final int pendingExp;
    private final int pendingMeso;
    private final int pendingCommonDrops;
    private final int pendingRareDrops;
    private final int playerPower;
    private final int recommendedPower;

    public IdleCombatSnapshot(int characterId, int stageId, String stageName, int elapsedSeconds, int totalKills,
                              int pendingExp, int pendingMeso, int pendingCommonDrops, int pendingRareDrops,
                              int playerPower, int recommendedPower) {
        this.characterId = characterId;
        this.stageId = stageId;
        this.stageName = stageName;
        this.elapsedSeconds = elapsedSeconds;
        this.totalKills = totalKills;
        this.pendingExp = pendingExp;
        this.pendingMeso = pendingMeso;
        this.pendingCommonDrops = pendingCommonDrops;
        this.pendingRareDrops = pendingRareDrops;
        this.playerPower = playerPower;
        this.recommendedPower = recommendedPower;
    }

    public int getCharacterId() {
        return characterId;
    }

    public int getStageId() {
        return stageId;
    }

    public String getStageName() {
        return stageName;
    }

    public int getElapsedSeconds() {
        return elapsedSeconds;
    }

    public int getTotalKills() {
        return totalKills;
    }

    public int getPendingExp() {
        return pendingExp;
    }

    public int getPendingMeso() {
        return pendingMeso;
    }

    public int getPendingCommonDrops() {
        return pendingCommonDrops;
    }

    public int getPendingRareDrops() {
        return pendingRareDrops;
    }

    public int getPlayerPower() {
        return playerPower;
    }

    public int getRecommendedPower() {
        return recommendedPower;
    }
}
