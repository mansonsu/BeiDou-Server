package org.gms.net.server.channel.handlers;

import org.gms.client.Character;
import org.gms.client.Client;
import org.gms.idle.IdleCombatService;
import org.gms.idle.IdleCombatSnapshot;
import org.gms.net.AbstractPacketHandler;
import org.gms.net.packet.InPacket;
import org.gms.util.PacketCreator;

public class IdleStageClaimHandler extends AbstractPacketHandler {
    @Override
    public void handlePacket(InPacket p, Client c) {
        Character chr = c.getPlayer();
        if (chr == null) {
            return;
        }

        try {
            IdleCombatSnapshot snapshot = IdleCombatService.getInstance().claim(chr);
            c.sendPacket(PacketCreator.idleStageResult(snapshot));
        } catch (RuntimeException ex) {
            c.sendPacket(PacketCreator.idleStageError(ex.getMessage()));
        }
    }
}
