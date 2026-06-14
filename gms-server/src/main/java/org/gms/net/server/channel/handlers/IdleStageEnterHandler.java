package org.gms.net.server.channel.handlers;

import org.gms.client.Character;
import org.gms.client.Client;
import org.gms.idle.IdleCombatService;
import org.gms.idle.IdleCombatSnapshot;
import org.gms.net.AbstractPacketHandler;
import org.gms.net.packet.InPacket;
import org.gms.util.PacketCreator;

public class IdleStageEnterHandler extends AbstractPacketHandler {
    @Override
    public void handlePacket(InPacket p, Client c) {
        Character chr = c.getPlayer();
        if (chr == null) {
            return;
        }

        try {
            int stageId = p.readInt();
            IdleCombatSnapshot snapshot = IdleCombatService.getInstance().enter(chr, stageId);
            c.sendPacket(PacketCreator.idleStageResult(IdleCombatService.ACTION_ENTER, true, snapshot, "已進入放置關卡"));
        } catch (RuntimeException ex) {
            c.sendPacket(PacketCreator.idleStageError(IdleCombatService.ACTION_ENTER, ex.getMessage()));
        }
    }
}
