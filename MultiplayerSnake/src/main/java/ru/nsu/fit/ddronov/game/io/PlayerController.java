package ru.nsu.fit.ddronov.game.io;

import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import lombok.Getter;
import me.ippolitov.fit.snakes.SnakesProto;
import ru.nsu.fit.ddronov.game.snake.SnakeMasterController;
import ru.nsu.fit.ddronov.game.snake.SnakeView;
import ru.nsu.fit.ddronov.game.io.datatypes.MessageWithSender;
import ru.nsu.fit.ddronov.game.io.datatypes.PlayerSignature;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PlayerController {
    private final int listenPort;
    @Getter
    private int myId;

    private final UnicastManager unicastManager;
    private final MulticastManager multicastManager;
    @Getter
    private final PlayersManager playersManager;
    @Getter
    private final AvailableGamesManager availableGamesManager;

    private final Lock roleLock = new ReentrantLock();
    @Getter
    private volatile SnakesProto.NodeRole role;

    private final Thread announceWorkerThread;
    private final Thread listenUnicastWorkerThread;
    private final Thread listenMulticastWorkerThread;
    private final Thread sendGameStateWorkerThread;

    @Getter
    private final String name;

    private final AtomicBoolean stopped = new AtomicBoolean(false);

    private static final Logger logger = Logger.getLogger(UnicastManager.class.getName());


    @Getter
    private final Subject<MessageWithSender> newMessageSubject = PublishSubject.create();
    @Getter
    private final Subject<SnakeView.Control> controlSubject = PublishSubject.create();
    @Getter
    private final Subject<SnakesProto.NodeRole> roleSubject = PublishSubject.create();

    private final SnakeMasterController snakeMasterController;

    private volatile SnakesProto.GameState state;

    void setRole(SnakesProto.NodeRole role) {
        roleLock.lock();
        this.role = role;
        roleLock.unlock();
    }

    public PlayerController(String name, int listenPort, SnakesProto.NodeRole role) throws IOException {
        this.listenPort = listenPort;
        this.name = name;
        this.role = role;
        roleSubject.onNext(role);

        state = SnakesProto.GameState.newBuilder()
                .setStateOrder(0)
                .setConfig(SnakesProto.GameConfig.newBuilder().setStateDelayMs(200))
                .setPlayers(SnakesProto.GamePlayers.getDefaultInstance())
                .build();


        DatagramSocket socket = new DatagramSocket(listenPort);
        unicastManager = new UnicastManager(socket);
        multicastManager = new MulticastManager(socket);
        playersManager = new PlayersManager(this::onPlayerDeadListener);
        availableGamesManager = new AvailableGamesManager(multicastManager);

        snakeMasterController = new SnakeMasterController(
                controlSubject.map(control -> {
                    if (control.getPlayerId() == null) {
                        control.setPlayerId(playersManager.getMyId());
                    }
                    return control;
                }),
                player -> {
//                    if (player.getRole() == SnakesProto.NodeRole.DEPUTY) {
//                        playersManager.changeRole(player.getId(), SnakesProto.NodeRole.VIEWER);
//                        selectDeputy();
//                    }
//                    setRole(SnakesProto.NodeRole.VIEWER);
////                    playersManager.changeRole(player.getId(), SnakesProto.NodeRole.VIEWER);
//                    unicastManager.sendPacket(
//                            player.getIpAddress(),
//                            player.getPort(),
//                            SnakesProto.GameMessage.newBuilder()
//                                    .setMsgSeq(0)
//                                    .setSenderId(myId)
//                                    .setRoleChange(SnakesProto.GameMessage.RoleChangeMsg.newBuilder()
//                                            .setSenderRole(SnakesProto.NodeRole.VIEWER)
//                                            .build()
//                                    )
//                                    .build()
//                    );
                }
        );

        controlSubject.subscribe(control -> {
            if (control.getPlayerId() == myId) {
                playersManager.getMaster().ifPresent(master -> {
                    if (master.getId() != myId) {
                        unicastManager.sendPacket(master.getIpAddress(), master.getPort(), SnakesProto.GameMessage.newBuilder()
                                .setMsgSeq(0)
                                .setSteer(SnakesProto.GameMessage.SteerMsg.newBuilder().setDirection(control.getDirection()))
                                .build()
                        );
                    }
                });
            }
        });

        new Thread(this::infoWorker).start();
        new Thread(this::pingWorker).start();

        announceWorkerThread = new Thread(this::announceWorker);
        announceWorkerThread.start();

        listenUnicastWorkerThread = new Thread(this::listenUnicastWorker);
        listenUnicastWorkerThread.start();

        listenMulticastWorkerThread = new Thread(this::listenMulticastWorker);
        listenMulticastWorkerThread.start();

        sendGameStateWorkerThread = new Thread(this::sendGameStateWorker);
        sendGameStateWorkerThread.start();

        myId = playersManager.getNextPlayerId();
        playersManager.setMyId(myId);


//        if (role != SnakesProto.NodeRole.MASTER) {
//            Observable.timer(3000, TimeUnit.MILLISECONDS).subscribeOn(Schedulers.io()).subscribe(
//                    time -> {
//                        var game = availableGamesManager.getGames().stream().findFirst();
//                        game.ifPresent(this::joinGame);
//                    }
//            );
//        }
    }

    public void createGame() {
        roleLock.lock();
        role = SnakesProto.NodeRole.MASTER;
        playersManager.updatePlayer(
                SnakesProto.GamePlayer.newBuilder()
                        .setName(name)
                        .setId(myId)
                        .setIpAddress("")
                        .setPort(listenPort)
                        .setScore(0)
                        .setRole(role)
                        .build()
        );
        roleLock.unlock();
    }

    public void stop() {
        stopped.set(true);

        unicastManager.stop();
        multicastManager.stop();
        playersManager.stop();
        availableGamesManager.stop();

        announceWorkerThread.interrupt();
        listenUnicastWorkerThread.interrupt();
        listenMulticastWorkerThread.interrupt();
        sendGameStateWorkerThread.interrupt();
    }

    private void selectDeputy() {
        playersManager.getNormal().ifPresent(
                newDeputy -> {
                    playersManager.changeRole(newDeputy.getId(), SnakesProto.NodeRole.DEPUTY);
                    unicastManager.sendPacket(
                            newDeputy.getIpAddress(),
                            newDeputy.getPort(),
                            SnakesProto.GameMessage.newBuilder()
                                    .setMsgSeq(0)
                                    .setRoleChange(SnakesProto.GameMessage.RoleChangeMsg.newBuilder()
                                            .setReceiverRole(SnakesProto.NodeRole.DEPUTY)
                                            .setSenderRole(SnakesProto.NodeRole.MASTER)
                                            .build()
                                    )
                                    .build()
                    );
                }
        );
    }

    private void onPlayerDeadListener(SnakesProto.GamePlayer deadPlayer) {
//      logger.warning(String.format("me is %s, he is %s", mySignature, new PlayerSignature(deadPlayer)));
        logger.warning(String.format("%s (%s): %s (%s) is dead", name, role, deadPlayer.getName(), deadPlayer.getRole()));
        // ???????? ?? ?????????? MASTER ??????????????, ?????? ?????????????????? DEPUTY. ?????????? ???? ???????????????? ???????????? DEPUTY ?????????? NORMAL-????, ?? ???????????????? ???? ???????? ???????????? DEPUTY ???????????????????? RoleChangeMsg (?????????????????? ???????????? ?? ?????????? DEPUTY ???? ?????????????????? StatusMsg, ???? ?????? ?????????? ???? ????????????).
        if (role == SnakesProto.NodeRole.MASTER) {
            if (deadPlayer.getRole() == SnakesProto.NodeRole.DEPUTY) {
                logger.info("MASTER saw DEPUTY dead, he selects NORMAL as DEPUTY");
                selectDeputy();
            }
        }
        // ???????? ?? ?????????? NORMAL ??????????????, ?????? ?????????????????? MASTER. ?????????? ???? ???????????????? ???????????????????? ?? ?????????????????????? ???????? ???? ?????????????????????? (DEPUTY), ??.?? ???????????????? ???????????????? ?????? unicast-?????????????????? ?? ?????????????? DEPUTY.
        if (role == SnakesProto.NodeRole.NORMAL) {
            if (deadPlayer.getRole() == SnakesProto.NodeRole.MASTER) {
                logger.info("NORMAL saw MASTER dead, he selects DEPUTY as MASTER");
                playersManager.getDeputy().ifPresent(
                        deputy -> playersManager.changeRole(deputy.getId(), SnakesProto.NodeRole.MASTER)
                );
            }
        }
        // ???????? ?? ?????????? DEPUTY ??????????????, ?????? ?????????????????? MASTER. ?????????? ???? ?????? ???????????????????? MASTER-???? (?????????? ???????????????????? ?????????? ???? ????????), ???????????????? ???????????? DEPUTY, ?? ???????????????? ???? ???????? ?????????????? ???????????? ???????????????????? RoleChangeMsg.
        if (role == SnakesProto.NodeRole.DEPUTY) {
            if (deadPlayer.getRole() == SnakesProto.NodeRole.MASTER) {
                logger.info("DEPUTY saw MASTER dead, he becomes MASTER");
                // System.out.println(playersManager.getPlayers().size());

                roleLock.lock();
                role = SnakesProto.NodeRole.MASTER;
                roleSubject.onNext(role);
                roleLock.unlock();
                playersManager.changeRole(myId, SnakesProto.NodeRole.MASTER);

                // System.out.println("Eat count " + state.getFoodsList());

                // ???????? ???????????? ????????, ?? ?????????????? ?????? ???????????? ???????????? ?? ???????????????? ?????????????? ?????????????????? ???? ???????? ?????? ???? ??????????, ?????? ???????????? ????????
                // ???? ????????????-?????????????? ?????? ???????????? ???????????? ?????? ???????????? ????????, ???????????? ???????????????? (?? ???????????? ?????? ????????)
                // WF
                boolean hasDeputy = false;
                for (var player : playersManager.getPlayers()) {
                    if (player.getId() == myId) {
                        continue;
                    }
                    if (!hasDeputy) {
                        playersManager.changeRole(player.getId(), SnakesProto.NodeRole.DEPUTY);
                    }
                    // System.out.printf("Telling %s he is %s%n", player.getName(), !hasDeputy ? SnakesProto.NodeRole.DEPUTY : SnakesProto.NodeRole.NORMAL);
                    unicastManager.sendPacket(
                            player.getIpAddress(),
                            player.getPort(),
                            SnakesProto.GameMessage.newBuilder()
                                    .setMsgSeq(0)
                                    .setRoleChange(
                                            SnakesProto.GameMessage.RoleChangeMsg.newBuilder()
                                                    .setSenderRole(SnakesProto.NodeRole.MASTER)
                                                    .setReceiverRole(!hasDeputy ? SnakesProto.NodeRole.DEPUTY : SnakesProto.NodeRole.NORMAL)
                                                    .build()
                                    )
                                    .setSenderId(myId)
                                    .setReceiverId(player.getId())
                                    .build()
                    );
                    hasDeputy = true;
                }
            }
        }
    }

    void sendPing(SnakesProto.GamePlayer player) {
        unicastManager.sendPacket(player.getIpAddress(), player.getPort(), SnakesProto.GameMessage.newBuilder()
                .setMsgSeq(0)
                .setPing(SnakesProto.GameMessage.PingMsg.getDefaultInstance())
                .build()
        );
    }

    private void pingWorker() {
        while (!stopped.get()) {
            if (role == SnakesProto.NodeRole.MASTER) {
                playersManager.getPlayers().forEach(this::sendPing);
            } else {
                playersManager.getMaster().ifPresent(this::sendPing);
            }
            try {
                Thread.sleep(state.getConfig().getPingDelayMs());
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void infoWorker() {
        while (!stopped.get()) {
            logger.info(
                    String.format("%s%d (%s/%s), game players: %d (%s)",
                            stopped.get() ? "DEAD, " : "",
                            myId,
                            name,
                            role,
                            playersManager.getPlayers().size(),
                            playersManager.getPlayers().stream()
                                    .map(player -> String.format("%d (%s/%s)", player.getId(), player.getName(), player.getRole()))
                                    .collect(Collectors.joining(", "))
                    )
            );
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void listenUnicastWorker() {
        while (true) {
//            logger.info(String.format("%s: stopped is %s", name, stopped));
            MessageWithSender msg;
            try {
                msg = unicastManager.receivePacket();
            } catch (InterruptedException e) {
                break;
            }
            var signature = new PlayerSignature(msg.getIp(), msg.getPort());
            playersManager.touchPlayer(signature);
            roleLock.lock();
            if (role == SnakesProto.NodeRole.MASTER) {
                if (msg.getMessage().hasSteer()) {
                    playersManager.getIdBySignature(signature).ifPresent(id -> controlSubject.onNext(
                            new SnakeView.Control(id, msg.getMessage().getSteer().getDirection())
                    ));
                }
                if (msg.getMessage().hasJoin()) {
                    var playerId = playersManager.getNextPlayerId();
                    unicastManager.sendPacket(
                            signature.getIp(),
                            signature.getPort(),
                            SnakesProto.GameMessage.newBuilder()
                                    .setMsgSeq(0)
                                    .setAck(SnakesProto.GameMessage.AckMsg.getDefaultInstance())
                                    .setReceiverId(playerId)
                                    .build()
                    );
                    if (playersManager.getDeputy().isEmpty()) {
                        playersManager.updatePlayer(
                                SnakesProto.GamePlayer.newBuilder()
                                        .setName(msg.getMessage().getJoin().getName())
                                        .setRole(SnakesProto.NodeRole.DEPUTY)
                                        .setId(playerId)
                                        .setIpAddress(msg.getIp())
                                        .setPort(msg.getPort())
                                        .setScore(0)
                                        .build()
                        );
                        unicastManager.sendPacket(
                                signature.getIp(),
                                signature.getPort(),
                                SnakesProto.GameMessage.newBuilder()
                                        .setMsgSeq(0)
                                        .setRoleChange(
                                                SnakesProto.GameMessage.RoleChangeMsg.newBuilder()
                                                        .setReceiverRole(SnakesProto.NodeRole.DEPUTY)
                                                        .build()
                                        )
                                        .build()
                        );
                    } else {
                        playersManager.updatePlayer(
                                SnakesProto.GamePlayer.newBuilder()
                                        .setName(msg.getMessage().getJoin().getName())
                                        .setRole(SnakesProto.NodeRole.NORMAL)
                                        .setId(playerId)
                                        .setIpAddress(msg.getIp())
                                        .setPort(msg.getPort())
                                        .setScore(0)
                                        .build()
                        );
                    }
                }
            }
            if (role != SnakesProto.NodeRole.MASTER) {
                if (msg.getMessage().hasState()) {
                    var state = msg.getMessage().getState().getState();
                    this.state = state;
                    state.getPlayers().getPlayersList().forEach(gamePlayer -> {
                        var newPlayer = SnakesProto.GamePlayer.newBuilder(gamePlayer);
                        if (gamePlayer.getIpAddress().length() == 0) {
                            newPlayer.setIpAddress(msg.getIp());
                        }
                        if (gamePlayer.getId() == myId) {
                            newPlayer.setIpAddress("");
                        }
                        playersManager.updatePlayer(newPlayer.build());
                    });
                }
            }
            if (msg.getMessage().hasAck() && msg.getMessage().hasReceiverId()) {
                myId = msg.getMessage().getReceiverId();
                playersManager.setMyId(myId);
            }
            if (msg.getMessage().hasRoleChange()) {
                var roleChange = msg.getMessage().getRoleChange();
                if (roleChange.hasReceiverRole()) {
                    logger.info("switched from " + role + " to " + roleChange.getReceiverRole());
                    role = roleChange.getReceiverRole();
                    roleSubject.onNext(role);
                    playersManager.changeRole(myId, roleChange.getReceiverRole());
                }
                if (roleChange.hasSenderRole()) {
                    logger.info("switched sender to " + roleChange.getSenderRole());
                    playersManager.changeRole(msg.getMessage().getSenderId(), roleChange.getSenderRole());
                }
            }
            newMessageSubject.onNext(msg);
            roleLock.unlock();
        }
    }

    private void listenMulticastWorker() {

    }

    private void sendGameStateWorker() {
        while (true) {
            if (role == SnakesProto.NodeRole.MASTER) {
//                logger.info("sending game state to " + playersManager.getPlayers().size() + " players");
                var oldState = SnakesProto.GameState.newBuilder(state)
                        .setPlayers(
                                SnakesProto.GamePlayers.newBuilder()
                                        .addAllPlayers(playersManager.getPlayers())
                        )
                        .setStateOrder(0)
                        .build();

                state = snakeMasterController.getNextState(oldState);
                state.getPlayers().getPlayersList().forEach(playersManager::updatePlayerWithoutTouch);
                if (playersManager.getDeputy().isEmpty()) {
                    selectDeputy();
                }

                var msg = SnakesProto.GameMessage.newBuilder()
                        .setState(
                                SnakesProto.GameMessage.StateMsg.newBuilder()
                                        .setState(state)
                                        .build()
                        )
                        .setMsgSeq(0)
                        .build();

                for (var player : playersManager.getPlayers()) {
                    unicastManager.sendPacket(
                            player.getIpAddress(),
                            player.getPort(),
                            msg
                    );
                }
            }

            try {
                Thread.sleep(state.getConfig().getStateDelayMs());
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void announceWorker() {
        while (true) {
            if (role == SnakesProto.NodeRole.MASTER) {
                availableGamesManager.announce(
                        SnakesProto.GameMessage.AnnouncementMsg.
                                newBuilder()
                                .setCanJoin(true)
                                .setConfig(SnakesProto.GameConfig.getDefaultInstance())
                                .setPlayers(
                                        SnakesProto.GamePlayers.newBuilder()
                                                .addAllPlayers(playersManager.getPlayers())
                                )
                                .build()
                );
            }
            try {
                Thread.sleep(Config.ANNOUNCE_INTERVAL_MS);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

//    private void joinGame(SnakesProto.GameMessage.AnnouncementMsg msg) {
//        var master = msg.getPlayers().getPlayersList()
//                .stream().filter(
//                        player -> player.getRole() == SnakesProto.NodeRole.MASTER
//                ).findAny();
//        master.ifPresent(gamePlayer -> joinGame(gamePlayer.getIpAddress(), gamePlayer.getPort()));
//    }

    public void joinGame(MessageWithSender announceWrapper) {
        joinGame(announceWrapper.getIp(), announceWrapper.getPort());
    }

    private void joinGame(String ip, int port) {
        unicastManager.sendPacket(
                ip,
                port,
                SnakesProto.GameMessage.newBuilder().setJoin(
                                SnakesProto.GameMessage.JoinMsg.newBuilder().setName(name).build()
                        )
                        .setMsgSeq(0)
                        .build()
        );
    }
}
