package org.gms.idle;

import org.gms.client.Character;
import org.gms.client.Job;
import org.gms.client.inventory.InventoryType;
import org.gms.client.inventory.Item;
import org.gms.client.inventory.WeaponType;
import org.gms.server.ItemInformationProvider;
import org.gms.server.life.ElementalEffectiveness;
import org.gms.server.life.Monster;
import org.gms.server.life.MonsterStats;

public final class IdleCombatDamageCalculator {
    private static final double MINIMUM_DAMAGE = 1.0D;

    public double calculateExpectedDamagePerAttack(Character chr, Monster monster, IdleDamageContext context) {
        if (chr == null || monster == null) {
            return 0.0D;
        }

        IdleDamageContext damageContext = context != null ? context : IdleDamageContext.defaults();
        MonsterStats monsterStats = monster.getStats();
        double maxAttributeAttack = calculateMaxAttributeAttack(chr);
        double minAttributeAttack = maxAttributeAttack * clamp(damageContext.getMasteryPercent() / 100.0D, 0.0D, 1.0D);
        double actualAttributeAttack = (maxAttributeAttack + minAttributeAttack) / 2.0D;
        double skillDamage = actualAttributeAttack * damageContext.getSkillPercent() / 100.0D;
        double additiveDamage = 1.0D + (damageContext.getDamagePercent()
                + (monsterStats.isBoss() ? damageContext.getBossDamagePercent() : damageContext.getNormalMonsterDamagePercent())) / 100.0D;
        double finalDamage = skillDamage * Math.max(0.0D, additiveDamage) * Math.max(0.0D, damageContext.getFinalDamageMultiplier());
        finalDamage *= calculateDefenseMultiplier(monsterStats, damageContext);
        finalDamage *= calculateElementMultiplier(monsterStats, damageContext);
        finalDamage *= calculateCriticalExpectation(damageContext);
        finalDamage *= calculateLevelDifferenceMultiplier(chr.getLevel(), monsterStats.getLevel());
        finalDamage *= calculateStarForceMultiplier(damageContext);
        finalDamage *= calculateArcaneForceMultiplier(damageContext);
        finalDamage *= calculateAuthenticForceMultiplier(damageContext);

        if (Double.isNaN(finalDamage) || finalDamage <= 0.0D) {
            return MINIMUM_DAMAGE;
        }
        return Math.max(MINIMUM_DAMAGE, finalDamage);
    }

    public int calculatePower(Character chr) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1.0D, calculateMaxAttributeAttack(chr)));
    }

    private double calculateMaxAttributeAttack(Character chr) {
        if (isMagicJob(chr.getJob())) {
            int magicAttack = Math.max(1, chr.getTotalMagic());
            return ((chr.getTotalInt() * 4.0D) + chr.getTotalLuk()) * resolveWeaponMultiplier(chr) * magicAttack / 100.0D;
        }
        return Math.max(1, chr.calculateMaxBaseDamage(Math.max(1, chr.getTotalWatk()), resolveWeaponType(chr)));
    }

    private WeaponType resolveWeaponType(Character chr) {
        Item weapon = chr.getInventory(InventoryType.EQUIPPED).getItem((short) -11);
        if (weapon != null) {
            return ItemInformationProvider.getInstance().getWeaponType(weapon.getItemId());
        }
        return defaultWeaponType(chr.getJob());
    }

    private double resolveWeaponMultiplier(Character chr) {
        return resolveWeaponType(chr).getMaxDamageMultiplier();
    }

    private WeaponType defaultWeaponType(Job job) {
        if (job.isA(Job.MAGICIAN) || job.isA(Job.BLAZEWIZARD1) || job.isA(Job.EVAN1)) {
            return WeaponType.STAFF;
        }
        if (job.isA(Job.BOWMAN) || job.isA(Job.WINDARCHER1)) {
            return WeaponType.BOW;
        }
        if (job.isA(Job.THIEF) || job.isA(Job.NIGHTWALKER1)) {
            return WeaponType.CLAW;
        }
        if (job.isA(Job.PIRATE) || job.isA(Job.THUNDERBREAKER1)) {
            return WeaponType.KNUCKLE;
        }
        if (job.isA(Job.ARAN1)) {
            return WeaponType.POLE_ARM_SWING;
        }
        return WeaponType.SWORD1H;
    }

    private boolean isMagicJob(Job job) {
        return job.isA(Job.MAGICIAN) || job.isA(Job.BLAZEWIZARD1) || job.isA(Job.EVAN1);
    }

    private double calculateDefenseMultiplier(MonsterStats monsterStats, IdleDamageContext context) {
        double rawDefenseRate = Math.max(monsterStats.getPDDamage(), monsterStats.getMDDamage()) / 100.0D;
        double effectiveDefenseRate = rawDefenseRate * (1.0D - clamp(context.getIgnoreDefenseMultiplier(), 0.0D, 1.0D));
        return Math.max(0.0D, 1.0D - effectiveDefenseRate);
    }

    private double calculateElementMultiplier(MonsterStats monsterStats, IdleDamageContext context) {
        ElementalEffectiveness effectiveness = monsterStats.getEffectiveness(context.getAttackElement());
        return switch (effectiveness) {
            case IMMUNE -> 0.0D;
            case STRONG -> 0.5D;
            case WEAK -> 1.5D;
            case NORMAL, NEUTRAL -> 1.0D;
        };
    }

    private double calculateCriticalExpectation(IdleDamageContext context) {
        double criticalRate = clamp(context.getCriticalRate(), 0.0D, 1.0D);
        double criticalDamage = Math.max(0.0D, context.getCriticalDamagePercent()) / 100.0D;
        return (1.0D - criticalRate) + criticalRate * (1.0D + criticalDamage);
    }

    private double calculateLevelDifferenceMultiplier(int characterLevel, int monsterLevel) {
        int diff = characterLevel - monsterLevel;
        if (diff >= 0) {
            return 1.0D + Math.min(0.2D, diff * 0.01D);
        }
        return Math.max(0.1D, 1.0D + diff * 0.02D);
    }

    private double calculateStarForceMultiplier(IdleDamageContext context) {
        int required = context.getRequiredStarForce();
        if (required <= 0) {
            return 1.0D;
        }
        double ratio = (double) context.getCharacterStarForce() / required;
        if (ratio >= 1.0D) {
            return 1.0D;
        }
        if (ratio >= 0.7D) {
            return 0.7D;
        }
        if (ratio >= 0.5D) {
            return 0.5D;
        }
        return 0.1D;
    }

    private double calculateArcaneForceMultiplier(IdleDamageContext context) {
        return calculateForceMultiplier(context.getCharacterArcaneForce(), context.getRequiredArcaneForce());
    }

    private double calculateAuthenticForceMultiplier(IdleDamageContext context) {
        return calculateForceMultiplier(context.getCharacterAuthenticForce(), context.getRequiredAuthenticForce());
    }

    private double calculateForceMultiplier(int characterForce, int requiredForce) {
        if (requiredForce <= 0) {
            return 1.0D;
        }
        double ratio = (double) characterForce / requiredForce;
        if (ratio >= 1.5D) {
            return 1.5D;
        }
        if (ratio >= 1.3D) {
            return 1.3D;
        }
        if (ratio >= 1.1D) {
            return 1.1D;
        }
        if (ratio >= 1.0D) {
            return 1.0D;
        }
        if (ratio >= 0.7D) {
            return 0.8D;
        }
        if (ratio >= 0.5D) {
            return 0.7D;
        }
        if (ratio >= 0.3D) {
            return 0.6D;
        }
        if (ratio >= 0.1D) {
            return 0.3D;
        }
        return 0.1D;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
