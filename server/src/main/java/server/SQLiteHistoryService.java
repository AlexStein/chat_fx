package server;

import java.sql.*;

public class SQLiteHistoryService implements HistoryService {

    private final Connection connection;
    private PreparedStatement psInsertHistory;
    private PreparedStatement psInsertPrivateHistory;

    /**
     * Инициируется сервис авторизации. Бросает исключение, чтобы сервер
     * понимал, что дальшеработать нельзя.
     *
     * @throws ClassNotFoundException Класс для работы с БД не найден
     * @throws SQLException Ошибка в создании запросов
     */
    public SQLiteHistoryService(Connection connection) throws ClassNotFoundException, SQLException {
        this.connection = connection;

        verifyDB();
        prepareAllStatements();
    }

    private void verifyDB() throws SQLException {
        String createHistory = "CREATE TABLE IF NOT EXISTS history (\n" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "sender VARCHAR(255) NOT NULL,\n" +
                "receiver VARCHAR(255),\n" +
                "message TEXT NOT NULL,\n" +
                "sent_at DATETIME DEFAULT CURRENT_TIMESTAMP);";
        try (Statement statement = connection.createStatement()) {
            statement.execute(createHistory);
        }
    }

    private void prepareAllStatements() throws SQLException {
        psInsertHistory = connection.prepareStatement("INSERT INTO history (sender,message) VALUES (?, ?);");
        psInsertPrivateHistory = connection.prepareStatement("INSERT INTO history (sender, receiver, message) VALUES (?, ?, ?);");
    }

    @Override
    public void saveMessage(String senderNickname, String message) {
        try {
            psInsertHistory.setString(1, senderNickname);
            psInsertHistory.setString(2, message);

            psInsertHistory.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void savePrivateMessage(String senderNickname, String ReceiverNickname, String message) {
        try {
            psInsertPrivateHistory.setString(1, senderNickname);
            psInsertPrivateHistory.setString(2, ReceiverNickname);
            psInsertPrivateHistory.setString(3, message);

            psInsertPrivateHistory.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
