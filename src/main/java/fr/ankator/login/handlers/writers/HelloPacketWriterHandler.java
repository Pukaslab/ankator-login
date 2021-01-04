package fr.ankator.login.handlers.writers;

import fr.ankator.common.network.packet.PacketMessage;
import fr.ankator.common.network.packet.PacketWriterHandler;
import fr.ankator.common.utils.Crypt;
import org.apache.mina.core.session.IoSession;

public class HelloPacketWriterHandler implements PacketWriterHandler {
    @Override
    public String compose(IoSession session, PacketMessage packet) {
        switch (packet.getType()) {
            case 'H':
                switch (packet.getAction()) {
                    case 'C':
                        return this.composeHelloConnection(session);
                }
                break;
        }

        // Return empty string to send packet without data. If null is returned, packet will not be sent
        return "";
    }

    protected String composeHelloConnection(IoSession session) {
        final String key = Crypt.randomHash(255);

        session.setAttribute("crypt_key", key);
        session.setAttribute("state", "waiting_version");

        return key;
    }
}
