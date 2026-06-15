package org.gms.net.server.channel.handlers;

import org.gms.client.Character;
import org.gms.client.Client;
import org.gms.idle.IdleCombatService;
import org.gms.net.AbstractPacketHandler;
import org.gms.net.packet.InPacket;
import org.gms.util.PacketCreator;

public class IdleStageStateHandler extends AbstractPacketHandler {
    @Override
    public void handlePacket(InPacket p, Client c) {
        Character chr = c.getPlayer();
        if (chr == null) {
            return;
        }

        try {
            IdleCombatService.getInstance().pushState(chr, IdleCombatService.ACTION_STATE, "放置狀態已更新");
        } catch (RuntimeException ex) {
            c.sendPacket(PacketCreator.idleStageError(IdleCombatService.ACTION_STATE, ex.getMessage()));
        }
    }
}
