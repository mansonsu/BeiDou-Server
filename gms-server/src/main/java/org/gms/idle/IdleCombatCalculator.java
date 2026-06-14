package org.gms.idle;

import org.gms.client.Character;

public final class IdleCombatCalculator {
    private static final int DROP_DENOMINATOR = 10000;

    public int calculatePower(Character chr) {
        int primaryStats = chr.getTotalStr() + chr.getTotalDex() + chr.getTotalInt() + chr.getTotalLuk();
        int attackStats = (chr.getTotalWatk() * 8) + (chr.getTotalMagic() * 4);
        return Math.max(1, primaryStats + attackStats + (chr.getLevel() * 10));
    }

    public IdleCombatResult calculate(Character chr, IdleStageConfig stage, long elapsedMillis) {
        if (elapsedMillis < stage.getKillIntervalMillis()) {
            return new IdleCombatResult((int) (elapsedMillis / 1000L), 0, 0, 0, 0, 0);
        }

        int rawKills = (int) Math.min(Integer.MAX_VALUE, elapsedMillis / stage.getKillIntervalMillis());
        int power = calculatePower(chr);
        double powerRatio = Math.max(0.25D, Math.min(2.0D, (double) power / Math.max(1, stage.getRecommendedPower())));
        int kills = Math.max(1, (int) Math.floor(rawKills * powerRatio));

        int levelDelta = chr.getLevel() - stage.getMonsterLevel();
        double levelModifier = levelDelta < -10 ? 0.5D : levelDelta > 20 ? 0.7D : 1.0D;
        int exp = safeMultiply(kills, (int) Math.max(1, Math.floor(stage.getBaseExpPerKill() * levelModifier)));
        int meso = safeMultiply(kills, Math.max(1, stage.getBaseMesoPerKill()));
        int commonDrops = expectedDrops(kills, stage.getCommonDropChancePerTenThousand());
        int rareDrops = expectedDrops(kills, stage.getRareDropChancePerTenThousand());

        return new IdleCombatResult((int) (elapsedMillis / 1000L), kills, exp, meso, commonDrops, rareDrops);
    }

    private int expectedDrops(int kills, int chancePerTenThousand) {
        if (chancePerTenThousand <= 0 || kills <= 0) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, ((long) kills * chancePerTenThousand) / DROP_DENOMINATOR);
    }

    private int safeMultiply(int left, int right) {
        return (int) Math.min(Integer.MAX_VALUE, (long) left * right);
    }
}
