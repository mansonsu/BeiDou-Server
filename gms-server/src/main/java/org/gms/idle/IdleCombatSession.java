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

    private IdleStageConfig stage;
    private long lastClaimMillis;
    private int lastGainedExp;
    private int lastGainedMeso;
    private int lastKills;

    public IdleCombatSession(IdleStageConfig stage) {
        this.stage = stage;
    }

    public synchronized IdleCombatSnapshot claim(Character chr, long nowMillis) {
        if (lastClaimMillis > 0L && nowMillis - lastClaimMillis < IdleCombatSettings.MIN_BATTLE_COMPLETE_REWARD_INTERVAL_MILLIS) {
            lastClaimMillis = nowMillis;
            lastGainedExp = 0;
            lastGainedMeso = 0;
            lastKills = 0;
            return snapshot();
        }

        lastClaimMillis = nowMillis;
        int kills = Randomizer.rand(IdleCombatSettings.MIN_BATTLE_COMPLETE_KILLS, IdleCombatSettings.MAX_BATTLE_COMPLETE_KILLS);

        lastGainedExp = 0;
        lastGainedMeso = 0;
        lastKills = kills;

        rollMonsterDrops(chr, kills);
        if (lastGainedExp > 0) {
            chr.gainExp(lastGainedExp, true, true);
        }
        if (lastGainedMeso > 0) {
            chr.gainMeso(lastGainedMeso, true, true, false);
        }

        return snapshot();
    }

    public synchronized void changeStage(IdleStageConfig nextStage) {
        this.stage = nextStage;
        this.lastGainedExp = 0;
        this.lastGainedMeso = 0;
        this.lastKills = 0;
        this.lastClaimMillis = 0L;
    }

    private IdleCombatSnapshot snapshot() {
        return new IdleCombatSnapshot(lastGainedExp, lastGainedMeso, lastKills);
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
        Map<Integer, Integer> rolledRewards = new LinkedHashMap<>();
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
                    addReward(rolledRewards, drop.itemId, quantity);
                }
            }
        }
        if (!rolledRewards.isEmpty()) {
            validateRewardSpace(chr, rolledRewards);
            grantItems(chr, rolledRewards);
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

        lastGainedExp = safeAdd(lastGainedExp, expValueToInteger(exp));
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
        lastGainedMeso = safeAdd(lastGainedMeso, mesos);
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

    private void addReward(Map<Integer, Integer> rewards, int itemId, int quantity) {
        if (itemId <= 0 || quantity <= 0) {
            return;
        }
        rewards.merge(itemId, quantity, this::safeAdd);
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
