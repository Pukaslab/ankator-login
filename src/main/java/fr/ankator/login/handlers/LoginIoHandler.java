package fr.ankator.login.handlers;

import fr.ankator.common.network.AbstractIoHandler;
import fr.ankator.common.network.packet.PacketHandler;
import fr.ankator.common.network.packet.PacketMessage;
import fr.ankator.login.handlers.readers.AccountPacketReaderHandler;
import fr.ankator.login.handlers.readers.DefaultPacketReaderHandler;
import fr.ankator.login.handlers.writers.AccountPacketWriterHandler;
import fr.ankator.login.handlers.writers.HelloPacketWriterHandler;
import org.apache.mina.core.session.IoSession;

public class LoginIoHandler extends AbstractIoHandler {
    protected final PacketHandler packetHandler;

    public LoginIoHandler() {
        this.packetHandler = new PacketHandler();

        // Register packet readers
        this.packetHandler.setDefaultReaderHandler(new DefaultPacketReaderHandler(this.packetHandler));
        this.packetHandler.addReaderHandler('A', new AccountPacketReaderHandler(this.packetHandler));

        // Register packet writers
        this.packetHandler.addWriterHandler('H', new HelloPacketWriterHandler());
        this.packetHandler.addWriterHandler('A', new AccountPacketWriterHandler());
    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {
        super.sessionOpened(session);

        this.packetHandler.compose(session, new PacketMessage('H', 'C'));
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        PacketMessage packet = (PacketMessage) message;

        LOGGER.info("IP: " + session.getRemoteAddress().toString() + " << " + packet.getData());

        this.packetHandler.handle(session, packet);
    }
}
