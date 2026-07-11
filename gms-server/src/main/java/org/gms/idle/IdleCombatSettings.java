package org.gms.idle;

public final class IdleCombatSettings {
    public static final int MIN_BATTLE_COMPLETE_KILLS = 5;
    public static final int MAX_BATTLE_COMPLETE_KILLS = 7;
    public static final int NORMAL_STAGES_BEFORE_BOSS = 9;
    public static final int BOSS_REWARD_MULTIPLIER = 5;
    public static final long MIN_BATTLE_COMPLETE_REWARD_INTERVAL_MILLIS = 2_000L;

    private IdleCombatSettings() {
    }
}
