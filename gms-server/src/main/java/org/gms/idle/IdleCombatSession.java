package org.gms.idle;

import org.gms.client.BuffStat;
import org.gms.client.Character;
import org.gms.client.inventory.manipulator.InventoryManipulator;
import org.gms.constants.inventory.ItemConstants;
import org.gms.server.ItemInformationProvider;
import org.gms.server.life.LifeFactory;
import org.gms.server.life.Monster;
import org.gms.server.life.MonsterDropEntry;
import org.gms.server.life.MonsterInformationProvider;
import org.gms.util.NumberTool;
import org.gms.util.Randomizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class IdleCombatSession {
    private static final String REWARD_OWNER = "";

    private final int characterId;
    private IdleStageConfig stage;
    private long lastTickMillis;
    private int totalKills;
    private int pendingExp;
    private int pendingMeso;
    private final Map<Integer, Integer> pendingRewards = new LinkedHashMap<>();

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
            rollMonsterDrops(chr, result.getKills());
        }
        return snapshot(result.getElapsedSeconds(), calculator.calculatePower(chr));
    }

    public synchronized IdleCombatSnapshot claim(Character chr, IdleCombatCalculator calculator, long nowMillis) {
        tick(chr, calculator, nowMillis);
        int claimedExp = pendingExp;
        int claimedMeso = pendingMeso;
        Map<Integer, Integer> claimedRewards = new LinkedHashMap<>(pendingRewards);
        validateRewardSpace(chr, claimedRewards);
        if (claimedExp > 0) {
            chr.gainExp(claimedExp, true, true);
        }
        if (claimedMeso > 0) {
            chr.gainMeso(claimedMeso, true, true, false);
        }
        grantItems(chr, claimedRewards);
        pendingExp = 0;
        pendingMeso = 0;
        pendingRewards.clear();
        return snapshot(0, calculator.calculatePower(chr));
    }

    public synchronized void changeStage(IdleStageConfig nextStage, long nowMillis) {
        this.stage = nextStage;
        this.lastTickMillis = nowMillis;
        this.totalKills = 0;
        this.pendingExp = 0;
        this.pendingMeso = 0;
        this.pendingRewards.clear();
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
                stage.getMapId(),
                stage.getMonsterIds(),
                snapshotRewards(),
                playerPower,
                stage.getRecommendedPower()
        );
    }

    private int safeAdd(int left, int right) {
        return (int) Math.min(Integer.MAX_VALUE, (long) left + right);
    }

    private void rollMonsterDrops(Character chr, int kills) {
        List<Integer> monsterIds = stage.getMonsterIds();
        if (monsterIds.isEmpty()) {
            return;
        }

        ItemInformationProvider itemInfo = ItemInformationProvider.getInstance();
        MonsterInformationProvider monsterInfo = MonsterInformationProvider.getInstance();
        float dropRate = chr.getDropRate();
        for (int i = 0; i < kills; i++) {
            int monsterId = monsterIds.get(Randomizer.nextInt(monsterIds.size()));
            rollMonsterExp(chr, monsterId);
            List<MonsterDropEntry> drops = monsterInfo.retrieveEffectiveDrop(monsterId);
            for (MonsterDropEntry drop : drops) {
                if (drop.itemId == 0) {
                    rollMesoDrop(chr, drop, dropRate);
                    continue;
                }
                if (!isIdleEligibleDrop(itemInfo, drop)) {
                    continue;
                }

                float cardRate = chr.getCardRate(drop.itemId);
                int dropChance = (int) Math.min((float) drop.chance * dropRate * cardRate, Integer.MAX_VALUE);
                if (Randomizer.nextInt(999999) < dropChance) {
                    int quantity = rollDropAmount(drop);
                    addPendingReward(drop.itemId, quantity);
                }
            }
        }
    }

    private void rollMonsterExp(Character chr, int monsterId) {
        Monster monster = LifeFactory.getMonster(monsterId);
        if (monster == null || monster.getExp() <= 0) {
            return;
        }

        double exp = monster.getExp();
        exp *= chr.getExpRate() * chr.getMobExpRate();

        Integer expBonus = chr.getBuffedValue(BuffStat.EXP_INCREASE);
        if (expBonus != null) {
            exp += expBonus;
        }

        Integer expBuff = chr.getBuffedValue(BuffStat.EXP_BUFF);
        if (expBuff != null) {
            exp *= 2.0D;
        }

        if (chr.isFamilyBuff()) {
            exp *= chr.getFamilyExp();
        }

        pendingExp = safeAdd(pendingExp, expValueToInteger(exp));
    }

    private void rollMesoDrop(Character chr, MonsterDropEntry drop, float dropRate) {
        if (drop.questid > 0) {
            return;
        }

        float cardRate = chr.getCardRate(drop.itemId);
        int dropChance = (int) Math.min((float) drop.chance * dropRate * cardRate, Integer.MAX_VALUE);
        if (Randomizer.nextInt(999999) >= dropChance) {
            return;
        }

        int mesos = rollDropAmount(drop);
        if (mesos <= 0) {
            return;
        }

        if (chr.getBuffedValue(BuffStat.MESOUP) != null) {
            mesos = NumberTool.doubleToInt(mesos * chr.getBuffedValue(BuffStat.MESOUP).doubleValue() / 100.0D);
        }
        mesos = NumberTool.floatToInt(mesos * chr.getMesoRate());
        if (mesos <= 0) {
            mesos = Integer.MAX_VALUE;
        }
        pendingMeso = safeAdd(pendingMeso, mesos);
    }

    private int rollDropAmount(MonsterDropEntry drop) {
        return drop.Maximum > drop.Minimum ? Randomizer.rand(drop.Minimum, drop.Maximum) : Math.max(1, drop.Minimum);
    }

    private int expValueToInteger(double exp) {
        if (exp > Integer.MAX_VALUE) {
            exp = Integer.MAX_VALUE;
        } else if (exp < 0) {
            exp = 0;
        }
        return (int) Math.round(exp);
    }

    private boolean isIdleEligibleDrop(ItemInformationProvider itemInfo, MonsterDropEntry drop) {
        return drop.itemId > 0
                && drop.questid <= 0
                && !itemInfo.isQuestItem(drop.itemId)
                && !itemInfo.isPartyQuestItem(drop.itemId);
    }

    private void addPendingReward(int itemId, int quantity) {
        if (itemId <= 0 || quantity <= 0) {
            return;
        }
        pendingRewards.merge(itemId, quantity, this::safeAdd);
    }

    private List<IdleRewardItem> snapshotRewards() {
        List<IdleRewardItem> rewards = new ArrayList<>();
        for (Map.Entry<Integer, Integer> reward : pendingRewards.entrySet()) {
            rewards.add(new IdleRewardItem(reward.getKey(), reward.getValue()));
        }
        return rewards;
    }

    private void validateRewardSpace(Character chr, Map<Integer, Integer> rewards) {
        List<Integer> itemIds = new ArrayList<>();
        List<Integer> quantities = new ArrayList<>();
        for (Map.Entry<Integer, Integer> reward : rewards.entrySet()) {
            addRewardCheck(itemIds, quantities, reward.getKey(), reward.getValue());
        }
        if (!chr.getAbstractPlayerInteraction().canHoldAllAfterRemoving(itemIds, quantities, Collections.emptyList(), Collections.emptyList())) {
            throw new IllegalStateException("背包空間不足，請先整理背包再領取放置獎勵");
        }
    }

    private void addRewardCheck(List<Integer> itemIds, List<Integer> quantities, int itemId, int quantity) {
        int remaining = quantity;
        int chunkSize = ItemConstants.isEquipment(itemId) ? 1 : Short.MAX_VALUE;
        while (itemId > 0 && remaining > 0) {
            itemIds.add(itemId);
            int checkQuantity = Math.min(chunkSize, remaining);
            quantities.add(checkQuantity);
            remaining -= checkQuantity;
        }
    }

    private void grantItems(Character chr, Map<Integer, Integer> rewards) {
        for (Map.Entry<Integer, Integer> reward : rewards.entrySet()) {
            grantItem(chr, reward.getKey(), reward.getValue());
        }
    }

    private void grantItem(Character chr, int itemId, int quantity) {
        if (itemId <= 0 || quantity <= 0) {
            return;
        }

        int remaining = quantity;
        int chunkSize = ItemConstants.isEquipment(itemId) ? 1 : Short.MAX_VALUE;
        while (remaining > 0) {
            short grantQuantity = (short) Math.min(chunkSize, remaining);
            if (!InventoryManipulator.addById(chr.getClient(), itemId, grantQuantity, REWARD_OWNER, -1)) {
                throw new IllegalStateException("放置獎勵道具發放失敗，請確認背包空間");
            }
            remaining -= grantQuantity;
        }
    }
}
