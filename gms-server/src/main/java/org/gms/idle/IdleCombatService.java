package org.gms.idle;

import org.gms.client.Character;
import org.gms.server.TimerManager;
import org.gms.util.PacketCreator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

public final class IdleCombatService {
    public static final byte ACTION_ENTER = 1;
    public static final byte ACTION_STATE = 2;
    public static final byte ACTION_CLAIM = 3;
    public static final byte ACTION_EXIT = 4;
    private static final long PUSH_INTERVAL_MILLIS = 10_000L;

    private static final IdleCombatService INSTANCE = new IdleCombatService();

    private final Map<Integer, IdleCombatSession> sessions = new ConcurrentHashMap<>();
    private final Map<Integer, Character> onlineCharacters = new ConcurrentHashMap<>();
    private final IdleCombatCalculator calculator = new IdleCombatCalculator();
    private ScheduledFuture<?> pushTask;

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
        onlineCharacters.put(chr.getId(), chr);
        ensurePushTask();
        return session.tick(chr, calculator, now);
    }

    public IdleCombatSnapshot state(Character chr) {
        IdleCombatSession session = requireSession(chr);
        onlineCharacters.put(chr.getId(), chr);
        ensurePushTask();
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
        onlineCharacters.remove(chr.getId());
        return snapshot;
    }

    public void pushState(Character chr, byte action, String message) {
        IdleCombatSnapshot snapshot = state(chr);
        chr.getClient().sendPacket(PacketCreator.idleStageResult(action, true, snapshot, message));
    }

    private synchronized void ensurePushTask() {
        if (pushTask != null && !pushTask.isCancelled()) {
            return;
        }
        pushTask = TimerManager.getInstance().register(this::pushOnlineStates, PUSH_INTERVAL_MILLIS, PUSH_INTERVAL_MILLIS);
    }

    private void pushOnlineStates() {
        if (sessions.isEmpty()) {
            stopPushTask();
            return;
        }

        List<Integer> inactiveCharacterIds = new ArrayList<>();
        for (Map.Entry<Integer, IdleCombatSession> entry : sessions.entrySet()) {
            int characterId = entry.getKey();
            Character chr = onlineCharacters.get(characterId);
            if (!isOnline(chr)) {
                inactiveCharacterIds.add(characterId);
                continue;
            }

            try {
                IdleCombatSnapshot snapshot = entry.getValue().tick(chr, calculator, System.currentTimeMillis());
                chr.getClient().sendPacket(PacketCreator.idleStageResult(ACTION_STATE, true, snapshot, "放置狀態已自動更新"));
            } catch (RuntimeException ex) {
                inactiveCharacterIds.add(characterId);
            }
        }

        for (Integer characterId : inactiveCharacterIds) {
            sessions.remove(characterId);
            onlineCharacters.remove(characterId);
        }

        if (sessions.isEmpty()) {
            stopPushTask();
        }
    }

    private boolean isOnline(Character chr) {
        return chr != null
                && chr.getClient() != null
                && chr.getClient().getPlayer() == chr;
    }

    private synchronized void stopPushTask() {
        if (pushTask != null) {
            TimerManager.getInstance().stop(pushTask);
            pushTask = null;
        }
    }

    private IdleStageConfig requireStage(Character chr, int stageId) {
        IdleStageConfig stage = IdleStageConfig.get(stageId);
        if (stage == null) {
            throw new IllegalArgumentException("未知的放置關卡：" + stageId);
        }
        if (chr.getLevel() < stage.getRequiredLevel()) {
            throw new IllegalArgumentException("角色等級不足，需要等級 " + stage.getRequiredLevel());
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
