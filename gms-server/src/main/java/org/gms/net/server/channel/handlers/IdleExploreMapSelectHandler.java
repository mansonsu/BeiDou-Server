package org.gms.net.server.channel.handlers;

import org.gms.client.Character;
import org.gms.client.Client;
import org.gms.idle.IdleExploreMapService;
import org.gms.idle.IdleExploreMapState;
import org.gms.net.AbstractPacketHandler;
import org.gms.net.packet.InPacket;
import org.gms.util.PacketCreator;

public class IdleExploreMapSelectHandler extends AbstractPacketHandler {
    @Override
    public void handlePacket(InPacket p, Client c) {
        Character chr = c.getPlayer();
        if (chr == null) {
            return;
        }

        try {
            int mapId = p.readInt();
            IdleExploreMapState state = IdleExploreMapService.getInstance().selectMap(chr, mapId);
            c.sendPacket(PacketCreator.idleExploreResult(IdleExploreMapService.ACTION_SELECT, true, state, "已選擇探索地圖"));
        } catch (RuntimeException ex) {
            c.sendPacket(PacketCreator.idleExploreError(IdleExploreMapService.ACTION_SELECT, ex.getMessage()));
        }
    }
}
