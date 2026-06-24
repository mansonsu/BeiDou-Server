package org.gms.idle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class IdleStageConfig {
    private final List<Integer> monsterIds;

    private IdleStageConfig(List<Integer> monsterIds) {
        this.monsterIds = Collections.unmodifiableList(new ArrayList<>(monsterIds));
    }

    public static IdleStageConfig fromExploreMap(int mapId, List<Integer> monsterIds) {
        if (monsterIds == null || monsterIds.isEmpty()) {
            throw new IllegalArgumentException("探索地圖沒有可用怪物: " + mapId);
        }
        return new IdleStageConfig(monsterIds);
    }

    public List<Integer> getMonsterIds() {
        return monsterIds;
    }
}
