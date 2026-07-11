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
            if (p.available() != 1) {
                throw new IllegalArgumentException("放置戰鬥封包格式不正確");
            }
            byte stageType = p.readByte();
            if (stageType != 0 && stageType != 1) {
                throw new IllegalArgumentException("放置戰鬥關卡類型不正確");
            }
            boolean isBossStage = stageType == 1;
            IdleCombatSnapshot snapshot = IdleCombatService.getInstance().claim(chr, isBossStage);
            c.sendPacket(PacketCreator.idleStageResult(snapshot));
        } catch (RuntimeException ex) {
            c.sendPacket(PacketCreator.idleStageError(ex.getMessage()));
        }
    }
}
