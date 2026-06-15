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
    private final int gainedExp;
    private final int gainedMeso;
    private final int mapId;
    private final List<Integer> monsterIds;
    private final List<IdleRewardItem> pendingRewards;
    private final int playerPower;
    private final int recommendedPower;
    private final int lastKills;
    private final int lastDamage;
    private final int lastMonsterId;
    private final int attackIntervalMillis;

    public IdleCombatSnapshot(int characterId, int stageId, String stageName, int elapsedSeconds, int totalKills,
                              int gainedExp, int gainedMeso, int mapId, List<Integer> monsterIds, List<IdleRewardItem> pendingRewards,
                              int playerPower, int recommendedPower, int lastKills, int lastDamage, int lastMonsterId,
                              int attackIntervalMillis) {
        this.characterId = characterId;
        this.stageId = stageId;
        this.stageName = stageName;
        this.elapsedSeconds = elapsedSeconds;
        this.totalKills = totalKills;
        this.gainedExp = gainedExp;
        this.gainedMeso = gainedMeso;
        this.mapId = mapId;
        this.monsterIds = Collections.unmodifiableList(new ArrayList<>(monsterIds));
        this.pendingRewards = Collections.unmodifiableList(new ArrayList<>(pendingRewards));
        this.playerPower = playerPower;
        this.recommendedPower = recommendedPower;
        this.lastKills = lastKills;
        this.lastDamage = lastDamage;
        this.lastMonsterId = lastMonsterId;
        this.attackIntervalMillis = attackIntervalMillis;
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

    public int getGainedExp() {
        return gainedExp;
    }

    public int getGainedMeso() {
        return gainedMeso;
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

    public int getLastKills() {
        return lastKills;
    }

    public int getLastDamage() {
        return lastDamage;
    }

    public int getLastMonsterId() {
        return lastMonsterId;
    }

    public int getAttackIntervalMillis() {
        return attackIntervalMillis;
    }
}
