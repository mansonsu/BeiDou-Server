package org.gms.net.server.channel.handlers;

import org.gms.client.Character;
import org.gms.client.Client;
import org.gms.constants.game.IdleConstants;
import org.gms.net.AbstractPacketHandler;
import org.gms.net.packet.InPacket;
import org.gms.util.PacketCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IdleInitHandler extends AbstractPacketHandler {
    private static final Logger log = LoggerFactory.getLogger(IdleInitHandler.class);

    @Override
    public void handlePacket(InPacket p, Client c) {
        Character chr = c.getPlayer();
        if (chr == null) {
            c.sendPacket(PacketCreator.idleInitResult(false, 0, "Player is not logged in"));
            return;
        }

        if (chr.isChangingMaps()) {
            c.sendPacket(PacketCreator.idleInitResult(false, chr.getMapId(), "Player is changing maps"));
            c.sendPacket(PacketCreator.enableActions());
            return;
        }

        try {
            if (chr.getMapId() != IdleConstants.DEFAULT_IDLE_MAP_ID) {
                chr.changeMap(IdleConstants.DEFAULT_IDLE_MAP_ID, 0);
            }
            c.sendPacket(PacketCreator.idleInitResult(true, IdleConstants.DEFAULT_IDLE_MAP_ID, ""));
        } catch (RuntimeException e) {
            log.warn("Failed to initialize idle mode for character {}", chr.getId(), e);
            c.sendPacket(PacketCreator.idleInitResult(false, chr.getMapId(), e.getMessage()));
            c.sendPacket(PacketCreator.enableActions());
        }
    }
}
