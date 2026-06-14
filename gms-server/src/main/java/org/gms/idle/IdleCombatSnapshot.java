package org.gms.idle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class IdleCombatSnapshot {
    private final int characterId;
    private final int stageId;
    private final String stageName;
    private final int elapsedSeconds;
    private final int totalKills;
    private final int pendingExp;
    private final int pendingMeso;
    private final int mapId;
    private final List<Integer> monsterIds;
    private final List<IdleRewardItem> pendingRewards;
    private final int playerPower;
    private final int recommendedPower;

    public IdleCombatSnapshot(int characterId, int stageId, String stageName, int elapsedSeconds, int totalKills,
                              int pendingExp, int pendingMeso, int mapId, List<Integer> monsterIds, List<IdleRewardItem> pendingRewards,
                              int playerPower, int recommendedPower) {
        this.characterId = characterId;
        this.stageId = stageId;
        this.stageName = stageName;
        this.elapsedSeconds = elapsedSeconds;
        this.totalKills = totalKills;
        this.pendingExp = pendingExp;
        this.pendingMeso = pendingMeso;
        this.mapId = mapId;
        this.monsterIds = Collections.unmodifiableList(new ArrayList<>(monsterIds));
        this.pendingRewards = Collections.unmodifiableList(new ArrayList<>(pendingRewards));
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

    public int getMapId() {
        return mapId;
    }

    public List<Integer> getMonsterIds() {
        return monsterIds;
    }

    public List<IdleRewardItem> getPendingRewards() {
        return pendingRewards;
    }

    public int getPlayerPower() {
        return playerPower;
    }

    public int getRecommendedPower() {
        return recommendedPower;
    }
}
