package org.gms.idle;

import org.gms.server.life.Element;

public final class IdleDamageContext {
    private static final double DEFAULT_SKILL_PERCENT = 100.0D;
    private static final double DEFAULT_MASTERY_PERCENT = 20.0D;

    private double skillPercent = DEFAULT_SKILL_PERCENT;
    private double damagePercent;
    private double normalMonsterDamagePercent;
    private double bossDamagePercent;
    private double finalDamageMultiplier = 1.0D;
    private double masteryPercent = DEFAULT_MASTERY_PERCENT;
    private double ignoreDefenseMultiplier;
    private double criticalRate;
    private double criticalDamagePercent;
    private int requiredStarForce;
    private int characterStarForce;
    private int requiredArcaneForce;
    private int characterArcaneForce;
    private int requiredAuthenticForce;
    private int characterAuthenticForce;
    private Element attackElement = Element.NEUTRAL;

    public static IdleDamageContext defaults() {
        return new IdleDamageContext();
    }

    public double getSkillPercent() {
        return skillPercent;
    }

    public double getDamagePercent() {
        return damagePercent;
    }

    public double getNormalMonsterDamagePercent() {
        return normalMonsterDamagePercent;
    }

    public double getBossDamagePercent() {
        return bossDamagePercent;
    }

    public double getFinalDamageMultiplier() {
        return finalDamageMultiplier;
    }

    public double getMasteryPercent() {
        return masteryPercent;
    }

    public double getIgnoreDefenseMultiplier() {
        return ignoreDefenseMultiplier;
    }

    public double getCriticalRate() {
        return criticalRate;
    }

    public double getCriticalDamagePercent() {
        return criticalDamagePercent;
    }

    public int getRequiredStarForce() {
        return requiredStarForce;
    }

    public int getCharacterStarForce() {
        return characterStarForce;
    }

    public int getRequiredArcaneForce() {
        return requiredArcaneForce;
    }

    public int getCharacterArcaneForce() {
        return characterArcaneForce;
    }

    public int getRequiredAuthenticForce() {
        return requiredAuthenticForce;
    }

    public int getCharacterAuthenticForce() {
        return characterAuthenticForce;
    }

    public Element getAttackElement() {
        return attackElement;
    }

    public IdleDamageContext withSkillPercent(double skillPercent) {
        this.skillPercent = skillPercent;
        return this;
    }

    public IdleDamageContext withDamagePercent(double damagePercent) {
        this.damagePercent = damagePercent;
        return this;
    }

    public IdleDamageContext withNormalMonsterDamagePercent(double normalMonsterDamagePercent) {
        this.normalMonsterDamagePercent = normalMonsterDamagePercent;
        return this;
    }

    public IdleDamageContext withBossDamagePercent(double bossDamagePercent) {
        this.bossDamagePercent = bossDamagePercent;
        return this;
    }

    public IdleDamageContext withFinalDamageMultiplier(double finalDamageMultiplier) {
        this.finalDamageMultiplier = finalDamageMultiplier;
        return this;
    }

    public IdleDamageContext withMasteryPercent(double masteryPercent) {
        this.masteryPercent = masteryPercent;
        return this;
    }

    public IdleDamageContext withIgnoreDefenseMultiplier(double ignoreDefenseMultiplier) {
        this.ignoreDefenseMultiplier = ignoreDefenseMultiplier;
        return this;
    }

    public IdleDamageContext withCritical(double criticalRate, double criticalDamagePercent) {
        this.criticalRate = criticalRate;
        this.criticalDamagePercent = criticalDamagePercent;
        return this;
    }

    public IdleDamageContext withStarForce(int characterStarForce, int requiredStarForce) {
        this.characterStarForce = characterStarForce;
        this.requiredStarForce = requiredStarForce;
        return this;
    }

    public IdleDamageContext withArcaneForce(int characterArcaneForce, int requiredArcaneForce) {
        this.characterArcaneForce = characterArcaneForce;
        this.requiredArcaneForce = requiredArcaneForce;
        return this;
    }

    public IdleDamageContext withAuthenticForce(int characterAuthenticForce, int requiredAuthenticForce) {
        this.characterAuthenticForce = characterAuthenticForce;
        this.requiredAuthenticForce = requiredAuthenticForce;
        return this;
    }

    public IdleDamageContext withAttackElement(Element attackElement) {
        this.attackElement = attackElement != null ? attackElement : Element.NEUTRAL;
        return this;
    }
}
