package server;

import java.sql.*;

public class SQLiteAuthService implements AuthService {

    private static Connection connection;
    private static Statement statement;
    private static PreparedStatement psInsertUser;
    private static PreparedStatement psSelectUser;
//    private static PreparedStatement psInsertHistory;
//    private static PreparedStatement psInsertPrivateHistory;
    private static ResultSet resultSet;

    public static void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:chat_fx.db");
        statement = connection.createStatement();
    }

    /**
     * Инициируется сервис авторизации. Бросает исключение, чтобы сервер
     * понимал, что дальшеработать нельзя.
     *
     * @throws ClassNotFoundException Класс для работы с БД не найден
     * @throws SQLException Ошибка в создании запросов
     */
    public SQLiteAuthService() throws ClassNotFoundException, SQLException {
        connect();
        verifyDB();

        prepareAllStatements();
        fillUsers();
    }

    private void verifyDB() {
        String createUsers = "CREATE TABLE IF NOT EXISTS users (\n" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "login VARCHAR(255) NOT NULL,\n" +
                "password BLOB NOT NULL,\n" +
                "nickname VARCHAR(255) NOT NULL,\n" +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP);";

        String createHistory = "CREATE TABLE IF NOT EXISTS history (\n" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "sender VARCHAR(255) NOT NULL,\n" +
                "receiver VARCHAR(255),\n" +
                "message TEXT NOT NULL,\n" +
                "sent_at DATETIME DEFAULT CURRENT_TIMESTAMP);";
        try {
            connection.setAutoCommit(false);
            statement.execute(createUsers);
            statement.execute(createHistory);
            connection.setAutoCommit(true);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Добавление демо-пользователей, если таблица пуста
     *
     * @throws SQLException Если чтото пошло не так
     */
    private void fillUsers() throws SQLException {

        resultSet = statement.executeQuery("SELECT count(*) FROM users;");
        if (resultSet.next()) {
            int count = resultSet.getInt(1);
            if (count == 0) {
                String[] demoUsers = {"qwe", "asd", "zxc"};
                connection.setAutoCommit(false);
                for (String name : demoUsers) {
                    psInsertUser.setString(1, name);
                    psInsertUser.setString(2, name);
                    psInsertUser.setString(3, name);
                    psInsertUser.addBatch();
                }
                psInsertUser.executeBatch();
                connection.setAutoCommit(true);
            }
        }
    }

    private static void prepareAllStatements() throws SQLException {
        psInsertUser = connection.prepareStatement("INSERT INTO users (login, password, nickname) VALUES (?, ?, ?);");
        psSelectUser = connection.prepareStatement("SELECT nickname FROM users WHERE login=? AND password=?;");
//        psInsertHistory = connection.prepareStatement("INSERT INTO history (sender,message) VALUES (?, ?);");
//        psInsertPrivateHistory = connection.prepareStatement("INSERT INTO history (sender, receiver, message) VALUES (?, ?, ?);");
    }

    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {
        try {
            psSelectUser.setString(1, login);
            psSelectUser.setString(2, password);
            resultSet = psSelectUser.executeQuery();

            if (resultSet.next()) {
                return resultSet.getString("nickname");
            }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean registration(String login, String password, String nickname) {
        String existingLogin = getNicknameByLoginAndPassword(login, password);
        if (existingLogin != null) {
            return false;
        }

        try {
            psInsertUser.setString(1, login);
            psInsertUser.setString(2, password);

            if (nickname == null || nickname.isEmpty()) {
                nickname = login;
            }

            psInsertUser.setString(3, nickname);
            int rowsInserted = psInsertUser.executeUpdate();
            return (rowsInserted > 0);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return true;
    }
}
