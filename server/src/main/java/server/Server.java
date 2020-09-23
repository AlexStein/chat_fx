package chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Server {
    private List<ClientHandler> clients;
    private AuthService authService;

    private int PORT = 8189;
    ServerSocket server = null;
    Socket socket = null;

    public Server() {
        clients = new Vector<>();
        authService = new SimpleAuthService();

        try {
            server = new ServerSocket(PORT);
            System.out.println("Сервер запущен");

            while (true) {
                socket = server.accept();
                System.out.println("Клиент подключился");

                new ClientHandler(this, socket);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
        String message = String.format(" %s %s : %s", formater.format(new Date()), sender.getNickname(), msg);
        for (ClientHandler c : clients) {
            if (!c.equals(sender)) {
                c.sendMsg(message);
            }
        }
        // Сообщение отправителю
        sender.sendMsg(String.format("Я : %s", msg));
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
    }


    public void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastClientList();
    }

    public void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
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
