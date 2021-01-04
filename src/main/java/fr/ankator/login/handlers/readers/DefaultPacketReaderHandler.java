package fr.ankator.login.handlers.readers;

import fr.ankator.common.config.Config;
import fr.ankator.common.database.repositories.UsersRepository;
import fr.ankator.common.models.User;
import fr.ankator.common.network.packet.PacketHandler;
import fr.ankator.common.network.packet.PacketMessage;
import fr.ankator.common.network.packet.PacketReaderHandler;
import fr.ankator.common.utils.Crypt;
import org.apache.mina.core.session.IoSession;

import java.util.Optional;

public class DefaultPacketReaderHandler implements PacketReaderHandler {
    protected final PacketHandler packetHandler;
    protected final UsersRepository usersRepository;

    public DefaultPacketReaderHandler(PacketHandler packetHandler) {
        this.packetHandler = packetHandler;
        this.usersRepository = new UsersRepository();
    }

    @Override
    public void parse(IoSession session, PacketMessage packet) {
        switch ((String) session.getAttribute("state", "")) {
            case "waiting_version" -> this.parseVersion(session, packet.getData());
            case "waiting_credentials" -> this.parseLogin(session, packet.getData());
            case "waiting_pseudo" -> this.parsePseudo(session, packet.getData());
        }
    }

    protected void parseVersion(IoSession session, String data) {
        if (!data.equals(Config.get("app.version"))) {
            this.packetHandler.compose(session, new PacketMessage('A', 'l', true, "v"));
            return;
        }

        session.setAttribute("state", "waiting_credentials");
    }

    protected void parseLogin(IoSession session, String data) {
        final String username = data.split("#1")[0];
        final String password = data.split("#1")[1];
        final Optional<User> userOrNull = this.usersRepository.findByUsername(username);

        System.out.println(Crypt.decodePassword(password, (String) session.getAttribute("crypt_key")));

        session.setAttribute("state", "queued");

        if (userOrNull.isPresent()) {
            User user = userOrNull.get();

            session.setAttribute("user", user);

            if (user.checkPassword(password, (String) session.getAttribute("crypt_key"))) {
                if (user.isAlreadyConnected(session)) {
                    this.packetHandler.compose(session, new PacketMessage('A', 'l', true, "a"));
                    return;
                }

                if (user.isAlreadyConnectedOnGameServer(session)) {
                    // TODO
                    return;
                }

                if (user.isBanned()) {
                    this.packetHandler.compose(session, new PacketMessage('A', 'l', true, "b"));
                    return;
                }

                if (user.isKicked()) {
                    this.packetHandler.compose(session, new PacketMessage('A', 'l', true, "k"));
                    return;
                }
            }
        }

        this.packetHandler.compose(session, new PacketMessage('A', 'l', true, "f"));
    }

    protected void parsePseudo(IoSession session, String data) {
        if (this.usersRepository.findByPseudo(data).isPresent()) {
            this.packetHandler.compose(session, new PacketMessage('A', 'l', true, "s"));
            return;
        }

        session.setAttribute("state", "queued");

        User user = (User) session.getAttribute("user");
        user.setPseudo(data);

        this.usersRepository.save(user);

        this.packetHandler.compose(session, new PacketMessage('A', 'd'));
        this.packetHandler.compose(session, new PacketMessage('A', 'c'));
        this.packetHandler.compose(session, new PacketMessage('A', 'H'));
        this.packetHandler.compose(session, new PacketMessage('A', 'l', "K0"));
    }
}
