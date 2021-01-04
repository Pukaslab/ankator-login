package fr.ankator.login.handlers.writers;

import fr.ankator.common.config.Config;
import fr.ankator.common.database.repositories.ServersRepository;
import fr.ankator.common.database.repositories.UsersRepository;
import fr.ankator.common.models.User;
import fr.ankator.common.network.packet.PacketMessage;
import fr.ankator.common.network.packet.PacketWriterHandler;
import org.apache.mina.core.session.IoSession;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Collectors;

public class AccountPacketWriterHandler implements PacketWriterHandler {
    protected final ServersRepository serversRepository;
    protected final UsersRepository usersRepository;

    public AccountPacketWriterHandler() {
        this.serversRepository = new ServersRepository();
        this.usersRepository = new UsersRepository();
    }

    @Override
    public String compose(IoSession session, PacketMessage packet) {
        switch (packet.getAction()) {
            case 'f':
                return this.composeQueue(session);
            case 'l':
                if (packet.isError()) {
                    switch (packet.getData()) {
                        case "v":
                            return this.composeBadVersion();
                        case "k":
                            return this.composeKick(session);
                    }
                }

                return "";
            case 'd':
                return this.composeUsername(session);
            case 'c':
                return this.composeCommunity();
            case 'H':
                return this.composeServersList();
            case 'x':
                return this.composerUserServersList(session);
        }

        // Return empty string to send packet without data. If null is returned, packet will not be sent
        return "";
    }

    protected String composeQueue(IoSession session) {
        if (!session.containsAttribute("user")) {
            session.closeNow();
            return null;
        }

        User user = (User) session.getAttribute("user");

        final int position = 1;
        final int totalSubscriber = 100;
        final int totalNonSubscriber = 100;
        final boolean isSubscriber = user.getSubscriptionDuration() > 0;
        final int queueId = -1;

        if (position < 2) {
            session.setAttribute("state", "queue_ended");
            return null;
        }

        return String.format("%s|%d|%d|%d|%d", position, totalSubscriber, totalNonSubscriber, isSubscriber ? 1 : 0, queueId);
    }

    protected String composeBadVersion() {
        return Config.get("app.version");
    }

    protected String composeKick(IoSession session) {
        if (!session.containsAttribute("user")) {
            session.closeNow();
            return null;
        }

        final User user = (User) session.getAttribute("user");
        final Duration kickUntil = Duration.between(user.getKickExpiredAt().toInstant(), Instant.now());
        final long days = kickUntil.toDays();
        final long hours = kickUntil.toHours() % 24;
        final long minutes = kickUntil.toMinutes() % 60;

        return String.format("%d|%d|%d", days, hours, minutes);
    }

    protected String composeUsername(IoSession session) {
        if (!session.containsAttribute("user")) {
            session.closeNow();
            return null;
        }

        return ((User) session.getAttribute("user")).getUsername();
    }

    protected String composeCommunity() {
        // International community
        return "2";
    }

    protected String composeServersList() {
        return this
                .serversRepository
                .getAll()
                .stream().map(s -> String.format(
                        "%d;%d;%d;%d",
                        s.getId(),
                        1, // TODO: connect to server conn to know current status
                        s.getCharacters().size() / s.getSize() * 100,
                        s.isCanLog() ? 1 : 0
                ))
                .collect(Collectors.joining("|"));
    }

    protected String composerUserServersList(IoSession session) {
        if (!session.containsAttribute("user")) {
            session.closeNow();
            return null;
        }

        User user = (User) session.getAttribute("user");

        return String.format(
                "%d|%s",
                user.getSubscriptionDuration(),
                this
                        .usersRepository
                        .getServersWithCharactersCount(user)
                        .entrySet()
                        .stream()
                        .map(o -> o.getKey() + "," + o.getValue())
                        .collect(Collectors.joining("|"))
        );
    }
}
