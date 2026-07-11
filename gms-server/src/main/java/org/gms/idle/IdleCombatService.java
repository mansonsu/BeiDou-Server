package org.gms.idle;

import org.gms.client.Character;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class IdleCombatService {
    private static final IdleCombatService INSTANCE = new IdleCombatService();

    private final Map<Integer, IdleCombatSession> sessions = new ConcurrentHashMap<>();

    private IdleCombatService() {
    }

    public static IdleCombatService getInstance() {
        return INSTANCE;
    }

    public void enterExploreMap(Character chr, IdleExploreMapState state) {
        if (state == null || !state.hasMap()) {
            throw new IllegalStateException("尚未選擇探索地圖");
        }
        IdleStageConfig stage = IdleStageConfig.fromExploreMap(state.getMapId(), state.getMonsterIds());
        enter(chr, stage);
    }

    private void enter(Character chr, IdleStageConfig stage) {
        sessions.compute(chr.getId(), (characterId, existing) -> {
            if (existing == null) {
                return new IdleCombatSession(stage);
            }
            existing.changeStage(stage);
            return existing;
        });
    }

    public IdleCombatSnapshot claim(Character chr, boolean isBossStage) {
        IdleCombatSession session = requireSession(chr);
        return session.claim(chr, System.currentTimeMillis(), isBossStage);
    }

    private IdleCombatSession requireSession(Character chr) {
        IdleCombatSession session = sessions.get(chr.getId());
        if (session == null) {
            throw new IllegalStateException("尚未選擇探索地圖");
        }
        return session;
    }
}
