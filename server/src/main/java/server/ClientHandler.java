package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.logging.Logger;

public class ClientHandler {

    private Logger logger;

    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private String nickname;
    private String login;

    public ClientHandler(Server server, Socket socket, Logger logger) {
        this.logger = logger;
        try {
            this.server = server;
            this.socket = socket;

            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    socket.setSoTimeout(120000);
                    //цикл аутентификации
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith("/auth ")) {
                            logger.info(String.format("Команда клиента: %s", str));

                            String[] token = str.split("\\s");
                            if (token.length < 3) {
                                continue;
                            }
                            String newNick = server
                                    .getAuthService()
                                    .getNicknameByLoginAndPassword(token[1], token[2]);
                            login = token[1];
                            if (newNick != null) {
                                if (!server.isLoginAuthenticated(login)) {
                                    nickname = newNick;
                                    sendMsg("/authok " + nickname);
                                    server.subscribe(this);
                                    logger.info(String.format("Клиента %s подключился", nickname));
                                    socket.setSoTimeout(0);
                                    break;
                                } else {
                                    sendMsg("С данной учетной записью уже зашли");
                                }
                            } else {
                                sendMsg("Неверный логин / пароль");
                            }
                        }

                        if (str.startsWith("/reg ")) {
                            logger.info(String.format("Команда клиента: %s", str));
                            String[] token = str.split("\\s");
                            if (token.length < 4) {
                                continue;
                            }

                            boolean b = server.getAuthService()
                                    .registration(token[1], token[2], token[3]);
                            if (b) {
                                sendMsg("/regok");
                            } else {
                                sendMsg("/regno");
                            }
                        }

                    }

                    //цикл работы
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith("/")) {
                            logger.info(String.format("Команда клиента: %s", str));
                            if (str.equals("/end")) {
                                out.writeUTF("/end");
                                break;
                            }

                            if (str.startsWith("/rename")) {
                                String[] token = str.split("\\s");
                                if (token.length < 2) {
                                    continue;
                                }
                                nickname = token[1];
                                boolean b = server.getAuthService()
                                    .updateNickname(login, nickname);
                                if (b) {
                                    server.rename_user();
                                }
                                continue;
                            }

                            if (str.startsWith("/w")) {
                                String[] token = str.split("\\s+", 3);
                                if (token.length < 3) {
                                    continue;
                                }
                                server.privateMsg(this, token[1], token[2]);
                            }
                        } else {
                            server.broadcastMsg(this, str);
                        }
                    }


                } catch (SocketTimeoutException e) {
                    sendMsg("/end");
                    logger.warning("Клиент отключен по таймауту");
                } catch (IOException e) {
                    logger.severe("Ошибка ввода вывода клиента");
                    logger.severe(e.getMessage());
                } finally {
                    logger.info("Клиент отключился");
                    server.unsubscribe(this);
                    try {
                        socket.close();
                    } catch (IOException e) {
                        logger.severe("Ошибка закрытия сокета клинета");
                        logger.severe(e.getMessage());
                    }
                }
            }).start();
        } catch (IOException e) {
            this.logger.severe("Ошибка в цикле работы клиента");
            this.logger.severe(e.getMessage());
        }

    }

    void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            logger.severe("Ошибка отправки сообщения");
            logger.severe(e.getMessage());
        }
    }

    public String getNickname() {
        return nickname;
    }

    public String getLogin() {
        return login;
    }
}
