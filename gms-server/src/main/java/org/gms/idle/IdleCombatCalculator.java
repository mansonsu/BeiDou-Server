package org.gms.idle;

import org.gms.client.Character;
import org.gms.server.life.LifeFactory;
import org.gms.server.life.Monster;

import java.util.List;

public final class IdleCombatCalculator {
    private final IdleCombatDamageCalculator damageCalculator = new IdleCombatDamageCalculator();

    public int calculatePower(Character chr) {
        return damageCalculator.calculatePower(chr);
    }

    public IdleCombatResult calculate(Character chr, IdleStageConfig stage, long elapsedMillis) {
        return calculate(chr, stage, elapsedMillis, 0.0D);
    }

    public IdleCombatResult calculate(Character chr, IdleStageConfig stage, long elapsedMillis, double carriedDamage) {
        if (elapsedMillis < stage.getKillIntervalMillis()) {
            return new IdleCombatResult((int) (elapsedMillis / 1000L), 0, Math.max(0.0D, carriedDamage), 0, firstMonsterId(stage.getMonsterIds()));
        }

        long attacks = elapsedMillis / stage.getKillIntervalMillis();
        double averageMonsterHp = calculateAverageMonsterHp(stage.getMonsterIds());
        double averageDamage = calculateAverageDamage(chr, stage.getMonsterIds());
        double totalDamage = Math.max(0.0D, carriedDamage) + (averageDamage * attacks);
        int kills = (int) Math.min(Integer.MAX_VALUE, Math.floor(totalDamage / averageMonsterHp));
        double remainingDamage = Math.max(0.0D, totalDamage - (kills * averageMonsterHp));

        return new IdleCombatResult(
                (int) (elapsedMillis / 1000L),
                kills,
                remainingDamage,
                damageToInteger(averageDamage),
                firstMonsterId(stage.getMonsterIds())
        );
    }

    private double calculateAverageMonsterHp(List<Integer> monsterIds) {
        double totalHp = 0.0D;
        int loadedMonsters = 0;
        for (Integer monsterId : monsterIds) {
            Monster monster = LifeFactory.getMonster(monsterId);
            if (monster == null || monster.getMaxHp() <= 0) {
                continue;
            }
            totalHp += monster.getMaxHp();
            loadedMonsters++;
        }
        return loadedMonsters > 0 ? totalHp / loadedMonsters : 1.0D;
    }

    private double calculateAverageDamage(Character chr, List<Integer> monsterIds) {
        double totalDamage = 0.0D;
        int loadedMonsters = 0;
        IdleDamageContext context = IdleDamageContext.defaults();
        for (Integer monsterId : monsterIds) {
            Monster monster = LifeFactory.getMonster(monsterId);
            if (monster == null) {
                continue;
            }
            totalDamage += damageCalculator.calculateExpectedDamagePerAttack(chr, monster, context);
            loadedMonsters++;
        }
        return loadedMonsters > 0 ? totalDamage / loadedMonsters : 1.0D;
    }

    private int firstMonsterId(List<Integer> monsterIds) {
        return monsterIds.isEmpty() ? 0 : monsterIds.get(0);
    }

    private int damageToInteger(double damage) {
        if (damage > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.max(1, (int) Math.round(damage));
    }
}
