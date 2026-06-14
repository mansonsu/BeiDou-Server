package org.gms.idle;

import org.gms.client.Character;
import org.gms.client.inventory.manipulator.InventoryManipulator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class IdleCombatSession {
    private static final String REWARD_OWNER = "";

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
        int claimedCommonDrops = pendingCommonDrops;
        int claimedRareDrops = pendingRareDrops;
        validateRewardSpace(chr, claimedCommonDrops, claimedRareDrops);
        if (claimedExp > 0) {
            chr.gainExp(claimedExp, true, true);
        }
        if (claimedMeso > 0) {
            chr.gainMeso(claimedMeso, true, true, false);
        }
        grantItem(chr, stage.getCommonRewardItemId(), claimedCommonDrops);
        grantItem(chr, stage.getRareRewardItemId(), claimedRareDrops);
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

    private void validateRewardSpace(Character chr, int commonDrops, int rareDrops) {
        List<Integer> itemIds = new ArrayList<>();
        List<Integer> quantities = new ArrayList<>();
        addRewardCheck(itemIds, quantities, stage.getCommonRewardItemId(), commonDrops);
        addRewardCheck(itemIds, quantities, stage.getRareRewardItemId(), rareDrops);
        if (!chr.getAbstractPlayerInteraction().canHoldAllAfterRemoving(itemIds, quantities, Collections.emptyList(), Collections.emptyList())) {
            throw new IllegalStateException("背包空間不足，請先整理背包再領取放置獎勵");
        }
    }

    private void addRewardCheck(List<Integer> itemIds, List<Integer> quantities, int itemId, int quantity) {
        int remaining = quantity;
        while (itemId > 0 && remaining > 0) {
            itemIds.add(itemId);
            int checkQuantity = Math.min(Short.MAX_VALUE, remaining);
            quantities.add(checkQuantity);
            remaining -= checkQuantity;
        }
    }

    private void grantItem(Character chr, int itemId, int quantity) {
        if (itemId <= 0 || quantity <= 0) {
            return;
        }

        int remaining = quantity;
        while (remaining > 0) {
            short grantQuantity = (short) Math.min(Short.MAX_VALUE, remaining);
            if (!InventoryManipulator.addById(chr.getClient(), itemId, grantQuantity, REWARD_OWNER, -1)) {
                throw new IllegalStateException("放置獎勵道具發放失敗，請確認背包空間");
            }
            remaining -= grantQuantity;
        }
    }
}
