package org.gms.idle;

import org.gms.client.Character;
import org.gms.server.maps.MapFactory;
import org.gms.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class IdleExploreMapService {
    public static final byte ACTION_SELECT = 1;

    private static final IdleExploreMapService INSTANCE = new IdleExploreMapService();

    private IdleExploreMapService() {
    }

    public static IdleExploreMapService getInstance() {
        return INSTANCE;
    }

    public IdleExploreMapState selectMap(Character chr, int mapId) {
        IdleExploreMapState state = createState(mapId, System.currentTimeMillis(), System.currentTimeMillis());
        save(chr.getId(), mapId);
        return state;
    }

    private IdleExploreMapState createState(int mapId, long startedAtMillis, long updatedAtMillis) {
        List<Integer> monsterIds = loadMonsterIds(mapId);
        String streetName = MapFactory.loadStreetName(mapId);
        String mapName = MapFactory.loadPlaceName(mapId);
        return new IdleExploreMapState(mapId, streetName, mapName, monsterIds, startedAtMillis, updatedAtMillis);
    }

    private List<Integer> loadMonsterIds(int mapId) {
        if (mapId <= 0) {
            throw new IllegalArgumentException("探索地圖 ID 無效: " + mapId);
        }

        List<Integer> monsterIds = MapFactory.getMonsterIdsFromWz(mapId);
        if (monsterIds.isEmpty()) {
            throw new IllegalArgumentException("探索地圖沒有可用怪物: " + mapId);
        }
        return new ArrayList<>(monsterIds);
    }

    private void save(int characterId, int mapId) {
        String sql = "INSERT INTO idle_exploration_state (characterid, explore_map_id, started_at, updated_at) "
                + "VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) "
                + "ON DUPLICATE KEY UPDATE explore_map_id = VALUES(explore_map_id), started_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, characterId);
            ps.setInt(2, mapId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("保存探索地圖失敗", ex);
        }
    }
}
