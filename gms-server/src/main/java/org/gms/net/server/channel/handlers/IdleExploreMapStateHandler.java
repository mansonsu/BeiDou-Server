package org.gms.net.server.channel.handlers;

import org.gms.client.Character;
import org.gms.client.Client;
import org.gms.idle.IdleExploreMapService;
import org.gms.idle.IdleExploreMapState;
import org.gms.net.AbstractPacketHandler;
import org.gms.net.packet.InPacket;
import org.gms.util.PacketCreator;

public class IdleExploreMapStateHandler extends AbstractPacketHandler {
    @Override
    public void handlePacket(InPacket p, Client c) {
        Character chr = c.getPlayer();
        if (chr == null) {
            return;
        }

        try {
            IdleExploreMapState state = IdleExploreMapService.getInstance().getState(chr);
            c.sendPacket(PacketCreator.idleExploreResult(IdleExploreMapService.ACTION_STATE, true, state, "探索地圖狀態已更新"));
        } catch (RuntimeException ex) {
            c.sendPacket(PacketCreator.idleExploreError(IdleExploreMapService.ACTION_STATE, ex.getMessage()));
        }
    }
}
