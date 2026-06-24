package org.gms.idle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class IdleExploreMapState {
    private final int mapId;
    private final String streetName;
    private final String mapName;
    private final List<Integer> monsterIds;
    private final long startedAtMillis;
    private final long updatedAtMillis;

    public IdleExploreMapState(int mapId, String streetName, String mapName,
                               List<Integer> monsterIds, long startedAtMillis, long updatedAtMillis) {
        this.mapId = mapId;
        this.streetName = streetName == null ? "" : streetName;
        this.mapName = mapName == null ? "" : mapName;
        this.monsterIds = Collections.unmodifiableList(new ArrayList<>(monsterIds));
        this.startedAtMillis = startedAtMillis;
        this.updatedAtMillis = updatedAtMillis;
    }

    public boolean hasMap() {
        return mapId > 0;
    }

    public int getMapId() {
        return mapId;
    }

    public String getStreetName() {
        return streetName;
    }

    public String getMapName() {
        return mapName;
    }

    public List<Integer> getMonsterIds() {
        return monsterIds;
    }

    public long getStartedAtMillis() {
        return startedAtMillis;
    }

    public long getUpdatedAtMillis() {
        return updatedAtMillis;
    }
}
