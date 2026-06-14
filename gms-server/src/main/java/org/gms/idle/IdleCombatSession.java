package org.gms.idle;

import org.gms.client.Character;

public final class IdleCombatSession {
    private final int characterId;
    private IdleStageConfig stage;
    private long lastTickMillis;
    private int totalKills;
    private int pendingExp;
    private int pendingMeso;
    private int pendingCommonDrops;
    private int pendingRareDrops;

    public IdleCombatSession(int characterId, IdleStageConfig stage, long nowMillis) {
        this.characterId = characterId;
        this.stage = stage;
        this.lastTickMillis = nowMillis;
    }

    public synchronized IdleCombatSnapshot tick(Character chr, IdleCombatCalculator calculator, long nowMillis) {
        long elapsedMillis = Math.max(0L, nowMillis - lastTickMillis);
        IdleCombatResult result = calculator.calculate(chr, stage, elapsedMillis);
        if (result.getKills() > 0) {
            lastTickMillis = nowMillis;
            totalKills = safeAdd(totalKills, result.getKills());
            pendingExp = safeAdd(pendingExp, result.getExp());
            pendingMeso = safeAdd(pendingMeso, result.getMeso());
            pendingCommonDrops = safeAdd(pendingCommonDrops, result.getCommonDrops());
            pendingRareDrops = safeAdd(pendingRareDrops, result.getRareDrops());
        }
        return snapshot(result.getElapsedSeconds(), calculator.calculatePower(chr));
    }

    public synchronized IdleCombatSnapshot claim(Character chr, IdleCombatCalculator calculator, long nowMillis) {
        tick(chr, calculator, nowMillis);
        int claimedExp = pendingExp;
        int claimedMeso = pendingMeso;
        if (claimedExp > 0) {
            chr.gainExp(claimedExp, true, true);
        }
        if (claimedMeso > 0) {
            chr.gainMeso(claimedMeso, true, true, false);
        }
        pendingExp = 0;
        pendingMeso = 0;
        pendingCommonDrops = 0;
        pendingRareDrops = 0;
        return snapshot(0, calculator.calculatePower(chr));
    }

    public synchronized void changeStage(IdleStageConfig nextStage, long nowMillis) {
        this.stage = nextStage;
        this.lastTickMillis = nowMillis;
        this.totalKills = 0;
        this.pendingExp = 0;
        this.pendingMeso = 0;
        this.pendingCommonDrops = 0;
        this.pendingRareDrops = 0;
    }

    private IdleCombatSnapshot snapshot(int elapsedSeconds, int playerPower) {
        return new IdleCombatSnapshot(
                characterId,
                stage.getStageId(),
                stage.getName(),
                elapsedSeconds,
                totalKills,
                pendingExp,
                pendingMeso,
                pendingCommonDrops,
                pendingRareDrops,
                playerPower,
                stage.getRecommendedPower()
        );
    }

    private int safeAdd(int left, int right) {
        return (int) Math.min(Integer.MAX_VALUE, (long) left + right);
    }
}
