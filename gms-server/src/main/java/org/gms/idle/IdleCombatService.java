package org.gms.idle;

import org.gms.client.Character;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class IdleCombatService {
    public static final byte ACTION_ENTER = 1;
    public static final byte ACTION_STATE = 2;
    public static final byte ACTION_CLAIM = 3;
    public static final byte ACTION_EXIT = 4;

    private static final IdleCombatService INSTANCE = new IdleCombatService();

    private final Map<Integer, IdleCombatSession> sessions = new ConcurrentHashMap<>();
    private final IdleCombatCalculator calculator = new IdleCombatCalculator();

    private IdleCombatService() {
    }

    public static IdleCombatService getInstance() {
        return INSTANCE;
    }

    public IdleCombatSnapshot enter(Character chr, int stageId) {
        IdleStageConfig stage = requireStage(chr, stageId);
        long now = System.currentTimeMillis();
        IdleCombatSession session = sessions.compute(chr.getId(), (characterId, existing) -> {
            if (existing == null) {
                return new IdleCombatSession(characterId, stage, now);
            }
            existing.changeStage(stage, now);
            return existing;
        });
        return session.tick(chr, calculator, now);
    }

    public IdleCombatSnapshot state(Character chr) {
        IdleCombatSession session = requireSession(chr);
        return session.tick(chr, calculator, System.currentTimeMillis());
    }

    public IdleCombatSnapshot claim(Character chr) {
        IdleCombatSession session = requireSession(chr);
        return session.claim(chr, calculator, System.currentTimeMillis());
    }

    public IdleCombatSnapshot exit(Character chr) {
        IdleCombatSession session = requireSession(chr);
        IdleCombatSnapshot snapshot = session.tick(chr, calculator, System.currentTimeMillis());
        sessions.remove(chr.getId());
        return snapshot;
    }

    private IdleStageConfig requireStage(Character chr, int stageId) {
        IdleStageConfig stage = IdleStageConfig.get(stageId);
        if (stage == null) {
            throw new IllegalArgumentException("找不到放置關卡：" + stageId);
        }
        if (chr.getLevel() < stage.getRequiredLevel()) {
            throw new IllegalArgumentException("角色等級不足，需求等級：" + stage.getRequiredLevel());
        }
        return stage;
    }

    private IdleCombatSession requireSession(Character chr) {
        IdleCombatSession session = sessions.get(chr.getId());
        if (session == null) {
            throw new IllegalStateException("尚未進入放置關卡");
        }
        return session;
    }
}
