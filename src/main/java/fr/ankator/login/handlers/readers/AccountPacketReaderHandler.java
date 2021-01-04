package fr.ankator.login.handlers.readers;

import fr.ankator.common.models.User;
import fr.ankator.common.network.packet.PacketHandler;
import fr.ankator.common.network.packet.PacketMessage;
import fr.ankator.common.network.packet.PacketReaderHandler;
import org.apache.mina.core.session.IoSession;

public class AccountPacketReaderHandler implements PacketReaderHandler {
    protected final PacketHandler packetHandler;

    public AccountPacketReaderHandler(PacketHandler packetHandler) {
        this.packetHandler = packetHandler;
    }

    @Override
    public void parse(IoSession session, PacketMessage packet) {
        switch (packet.getAction()) {
            case 'f' -> this.parseQueue(session);
        }
    }

    protected void parseQueue(IoSession session) {
        if (session.getAttribute("state") == "queued") {
            this.packetHandler.compose(session, new PacketMessage('A', 'f'));
        }

        if (session.getAttribute("state") == "queue_ended") {
            User user = (User) session.getAttribute("user");

            if (user.getPseudo() == null) {
                session.setAttribute("state", "waiting_pseudo");

                this.packetHandler.compose(session, new PacketMessage('A', 'l', true, "r"));
            } else {
                this.packetHandler.compose(session, new PacketMessage('A', 'd'));
                this.packetHandler.compose(session, new PacketMessage('A', 'c'));
                this.packetHandler.compose(session, new PacketMessage('A', 'H'));
                this.packetHandler.compose(session, new PacketMessage('A', 'l', "K0"));
            }
        }
    }
}
