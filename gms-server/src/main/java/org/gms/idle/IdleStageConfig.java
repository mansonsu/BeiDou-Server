package org.gms.idle;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class IdleStageConfig {
    private static final Map<Integer, IdleStageConfig> STAGES;

    static {
        Map<Integer, IdleStageConfig> stages = new LinkedHashMap<>();
        register(stages, new IdleStageConfig(20000, "弓箭手訓練場", 1, 40, 8, 8, 12, 5000, 100100));
        register(stages, new IdleStageConfig(20010, "森林小徑", 10, 140, 18, 22, 35, 5000, 1110100));
        register(stages, new IdleStageConfig(20020, "燃燒木道", 25, 380, 35, 70, 90, 5000, 1110101));
        STAGES = Collections.unmodifiableMap(stages);
    }

    private final int stageId;
    private final String name;
    private final int requiredLevel;
    private final int recommendedPower;
    private final int monsterLevel;
    private final int baseExpPerKill;
    private final int baseMesoPerKill;
    private final int killIntervalMillis;
    private final int monsterId;

    private IdleStageConfig(int stageId, String name, int requiredLevel, int recommendedPower, int monsterLevel,
                            int baseExpPerKill, int baseMesoPerKill, int killIntervalMillis, int monsterId) {
        this.stageId = stageId;
        this.name = name;
        this.requiredLevel = requiredLevel;
        this.recommendedPower = recommendedPower;
        this.monsterLevel = monsterLevel;
        this.baseExpPerKill = baseExpPerKill;
        this.baseMesoPerKill = baseMesoPerKill;
        this.killIntervalMillis = killIntervalMillis;
        this.monsterId = monsterId;
    }

    private static void register(Map<Integer, IdleStageConfig> stages, IdleStageConfig config) {
        stages.put(config.stageId, config);
    }

    public static IdleStageConfig get(int stageId) {
        return STAGES.get(stageId);
    }

    public int getStageId() {
        return stageId;
    }

    public String getName() {
        return name;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public int getRecommendedPower() {
        return recommendedPower;
    }

    public int getMonsterLevel() {
        return monsterLevel;
    }

    public int getBaseExpPerKill() {
        return baseExpPerKill;
    }

    public int getBaseMesoPerKill() {
        return baseMesoPerKill;
    }

    public int getKillIntervalMillis() {
        return killIntervalMillis;
    }

    public int getMonsterId() {
        return monsterId;
    }
}
