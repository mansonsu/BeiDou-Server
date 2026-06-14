package org.gms.idle;

import org.gms.client.Character;

public final class IdleCombatCalculator {
    public int calculatePower(Character chr) {
        int primaryStats = chr.getTotalStr() + chr.getTotalDex() + chr.getTotalInt() + chr.getTotalLuk();
        int attackStats = (chr.getTotalWatk() * 8) + (chr.getTotalMagic() * 4);
        return Math.max(1, primaryStats + attackStats + (chr.getLevel() * 10));
    }

    public IdleCombatResult calculate(Character chr, IdleStageConfig stage, long elapsedMillis) {
        if (elapsedMillis < stage.getKillIntervalMillis()) {
            return new IdleCombatResult((int) (elapsedMillis / 1000L), 0);
        }

        int rawKills = (int) Math.min(Integer.MAX_VALUE, elapsedMillis / stage.getKillIntervalMillis());
        int power = calculatePower(chr);
        double powerRatio = Math.max(0.25D, Math.min(2.0D, (double) power / Math.max(1, stage.getRecommendedPower())));
        int kills = Math.max(1, (int) Math.floor(rawKills * powerRatio));

        return new IdleCombatResult((int) (elapsedMillis / 1000L), kills);
    }
}
