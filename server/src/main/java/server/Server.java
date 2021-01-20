package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.*;

public class Server {

    private static final Logger logger = Logger.getLogger(Server.class.getName());

    private List<ClientHandler> clients;
    private Connection connection;
    private AuthService authService;
    private HistoryService historyService;

    private int PORT = 8189;
    ServerSocket server = null;
    Socket socket = null;

    public Server() {
        clients = new Vector<>();

        Handler fileHandler;
        try {
            fileHandler = new FileHandler("server_%g.log",10 * 1024, 5, true);
        } catch (IOException e) {
            e.printStackTrace();
            return;  // Выход
        }
        fileHandler.setFormatter(new SimpleFormatter());

        logger.addHandler(fileHandler);
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.INFO);

        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:chat_fx.db");
            authService = new SQLiteAuthService(connection);
        } catch (ClassNotFoundException | SQLException e) {
            logger.severe("Ошибка подключения к базе данных");
            logger.severe(e.getMessage());
            logger.info("Выход");
            return;  // Выход
        }

        try {
            historyService = new SQLiteHistoryService(connection);
        } catch (ClassNotFoundException | SQLException e) {
            // Если чтото не так, то будут дальше ошибки при
            // сохранении. Это допустимо.
            // Сервер может работать и без истрии
            logger.severe("Ошибка запуска истории чатов");
            logger.severe(e.getMessage());
        }

        try {
            server = new ServerSocket(PORT);
            logger.info("Сервер запущен");

            while (true) {
                socket = server.accept();
                logger.info("Клиент подключился");
                new ClientHandler(this, socket, logger);
            }

        } catch (IOException e) {
            logger.severe("Ошибка сервера");
            logger.severe(e.getMessage());

        } finally {
            try {
                server.close();
            } catch (IOException e) {
                logger.severe("Ошибка закрытия сокета сервера");
                logger.severe(e.getMessage());
            }
        }
        logger.info("Выход");
    }

    public AuthService getAuthService() {
        return authService;
    }

    /**
     * Отправка общего сообщения пользователя
     *
     * @param sender Отправитель
     * @param msg    Текст сообщения
     */
    public void broadcastMsg(ClientHandler sender, String msg) {
        SimpleDateFormat formater = new SimpleDateFormat("HH:mm:ss");
        String message = String.format("%s %s : %s", formater.format(new Date()), sender.getNickname(), msg);
        for (ClientHandler c : clients) {
            if (!c.equals(sender)) {
                c.sendMsg(message);
            }
        }
        // Сообщение отправителю
        sender.sendMsg(String.format("%s Я : %s", formater.format(new Date()), msg));
        historyService.saveMessage(sender.getNickname(), msg);
        logger.fine(String.format("Сообщение %s: %s", sender.getNickname(), msg));
    }

    /**
     * Сообщения сервера
     *
     * @param msg Текст информационного сообщения
     */
    public void broadcastServerMsg(String msg) {
        String message = String.format("* %s *", msg);
        for (ClientHandler c : clients) {
            c.sendMsg(message);
        }
        logger.fine(String.format("Сообщение сервера: %s", msg));
    }

    /**
     * Отправка личного сообщения пользователю
     *
     * @param sender         Отправитель
     * @param targetNickname Имя получателя
     * @param msg            Текст сообщения
     */
    public void privateMsg(ClientHandler sender, String targetNickname, String msg) {
        boolean sentSuccessfully = false;

        for (ClientHandler c : clients) {
            if (c.getNickname().equals(targetNickname)) {
                String message = String.format("Личное от %s : %s", sender.getNickname(), msg);
                c.sendMsg(message);
                sentSuccessfully = true;
                break;
            }
        }

        // Сообщение отправителю
        String message = String.format("Я : (личное для %s) %s", targetNickname, msg);
        if (!sentSuccessfully) {
            message = String.format("Пользователь %s не найден", targetNickname);
        }
        sender.sendMsg(message);
        historyService.savePrivateMessage(sender.getNickname(), targetNickname, msg);
    }

    public void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastClientList();
    }

    public void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastClientList();
    }

    public void rename_user() {
        broadcastClientList();
    }

    public boolean isLoginAuthenticated(String login) {
        for (ClientHandler c : clients) {
            if (c.getLogin().equals(login)) {
                return true;
            }
        }
        return false;
    }

    private void broadcastClientList() {
        StringBuilder sb = new StringBuilder("/clientlist ");
        for (ClientHandler c : clients) {
            sb.append(c.getNickname()).append(" ");
        }

        String msg = sb.toString();
        for (ClientHandler c : clients) {
            c.sendMsg(msg);
        }
    }

}
