package org.gms.net.server.handlers.login;

import org.gms.client.Client;
import org.gms.manager.ServerManager;
import org.gms.net.PacketHandler;
import org.gms.net.packet.InPacket;
import org.gms.net.server.Server;
import org.gms.net.server.coordinator.session.Hwid;
import org.gms.service.auth.GameLoginTokenService;
import org.gms.util.HexTool;
import org.gms.util.PacketCreator;

public final class ExternalTokenLoginHandler implements PacketHandler {
    @Override public boolean validateState(Client c) { return !c.isLoggedIn(); }
    @Override public void handlePacket(InPacket p, Client c) {
        String token=p.readString();
        byte[] hwidBytes=p.available()>=4?p.readBytes(4):new byte[]{0,0,0,0};
        Integer accountId=ServerManager.getApplicationContext().getBean(GameLoginTokenService.class).consume(token);
        if(accountId==null){c.sendPacket(PacketCreator.getLoginFailed(4));return;}
        int result=c.loginExternal(accountId,new Hwid(HexTool.toCompactHexString(hwidBytes)));
        if(result!=0){c.sendPacket(PacketCreator.getLoginFailed(result));return;}
        if(c.finishLogin()!=0){c.sendPacket(PacketCreator.getLoginFailed(7));return;}
        c.checkChar(c.getAccID()); c.sendPacket(PacketCreator.getAuthSuccess(c)); Server.getInstance().registerLoginState(c);
    }
}
