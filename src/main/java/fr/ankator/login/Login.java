package fr.ankator.login;

import fr.ankator.common.config.Config;
import fr.ankator.common.database.repositories.UsersRepository;
import fr.ankator.common.models.User;
import fr.ankator.common.network.IoServer;
import fr.ankator.common.network.packet.PacketCodecFactory;
import fr.ankator.login.handlers.LoginIoHandler;
import org.apache.mina.filter.codec.ProtocolCodecFilter;

import java.io.IOException;

public class Login {
    public static void main(String[] args) throws IOException {
        Config.load("login.properties");

        UsersRepository usersRepository = new UsersRepository();
        User user = new User();

        user.setUsername("test");
        user.setPassword("$2a$10$0DXCyZY8RCzGB23EkADciOglLy3D4nzQsTtEAD5tmCgRJMz96ulD6");

        usersRepository.save(user);

        final IoServer server = new IoServer(2205, 600, new LoginIoHandler());
        server.addLastFilter("packet", new ProtocolCodecFilter(new PacketCodecFactory()));
        server.start();
    }
}
