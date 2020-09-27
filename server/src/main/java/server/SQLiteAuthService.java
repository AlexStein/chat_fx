package server;

import java.sql.*;

public class SQLiteAuthService implements AuthService {

    private Connection connection;
    private PreparedStatement psInsertUser;
    private PreparedStatement psSelectUser;
    private PreparedStatement psUpdateUser;
    private ResultSet resultSet;

    /**
     * Инициируется сервис авторизации. Бросает исключение, чтобы сервер
     * понимал, что дальшеработать нельзя.
     *
     * @throws ClassNotFoundException Класс для работы с БД не найден
     * @throws SQLException Ошибка в создании запросов
     */
    public SQLiteAuthService(Connection connection) throws ClassNotFoundException, SQLException {
        this.connection = connection;

        verifyDB();
        prepareAllStatements();
        fillUsers();
    }

    private void verifyDB() throws SQLException {
        String createUsers = "CREATE TABLE IF NOT EXISTS users (\n" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "login VARCHAR(255) NOT NULL,\n" +
                "password BLOB NOT NULL,\n" +
                "nickname VARCHAR(255) NOT NULL,\n" +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP);";

        try (Statement statement = connection.createStatement()) {
            statement.execute(createUsers);
        }
    }

    /**
     * Добавление демо-пользователей, если таблица пуста
     *
     * @throws SQLException Если чтото пошло не так
     */
    private void fillUsers() throws SQLException {
        Statement statement = connection.createStatement();
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
        try {
            resultSet.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void prepareAllStatements() throws SQLException {
        psInsertUser = connection.prepareStatement("INSERT INTO users (login, password, nickname) VALUES (?, ?, ?);");
        psUpdateUser = connection.prepareStatement("UPDATE users SET nickname = ? WHERE login=?;");
        psSelectUser = connection.prepareStatement("SELECT nickname FROM users WHERE login=? AND password=?;");
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

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                resultSet.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
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

        return false;
    }

    @Override
    public boolean updateNickname(String login, String nickname) {
        try {
            psUpdateUser.setString(1, nickname);
            psUpdateUser.setString(2, login);
            int rowsUpdated = psUpdateUser.executeUpdate();
            return (rowsUpdated > 0);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}
