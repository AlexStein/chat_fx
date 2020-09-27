package server;

public interface HistoryService {

    /**
     * Сохранить сообщение пользователя в историю
     *
     * @param senderNickname Ник отправителя
     * @param message        Сообщение
     */
    void saveMessage(String senderNickname, String message);

    /**
     * Сохранить сообщение приватное пользователя в историю
     *
     * @param senderNickname Ник отправителя
     * @param ReceiverNickname Ник получателя
     * @param message        Сообщение
     */
    void savePrivateMessage(String senderNickname, String ReceiverNickname, String message);

}
