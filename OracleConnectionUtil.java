以下是一个实现了线程安全的 Oracle 数据库工具类。该工具类使用双重检查锁机制（Double-Checked Locking）来确保 `Connection` 对象只初始化一次。如果 `Connection` 已经存在，直接返回现有连接；如果不存在，则重新初始化连接。

```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class OracleConnectionUtil {

    // Oracle 连接信息
    private static final String URL = "jdbc:oracle:thin:@yourhost:1521:orcl";
    private static final String USER = "yourusername";
    private static final String PASSWORD = "yourpassword";

    // 用于存储单例的 Connection 实例
    private static volatile Connection connection;

    // 私有构造函数，防止实例化
    private OracleConnectionUtil() {}

    /**
     * 获取数据库连接，确保线程安全
     */
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            synchronized (OracleConnectionUtil.class) {
                if (connection == null || connection.isClosed()) {
                    connection = DriverManager.getConnection(URL, USER, PASSWORD);
                }
            }
        }
        return connection;
    }

    /**
     * 关闭数据库连接
     */
    public static void closeConnection() {
        if (connection != null) {
            synchronized (OracleConnectionUtil.class) {
                try {
                    if (!connection.isClosed()) {
                        connection.close();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    connection = null; // 清空连接
                }
            }
        }
    }
}
```

### 代码说明
1. **双重检查锁**：`getConnection()` 方法使用了双重检查锁（Double-Checked Locking）来确保线程安全。在外层和内层都判断 `connection` 是否为 `null` 或已关闭，确保只在需要时才进行同步操作，提升效率。
2. **`volatile` 关键字**：`connection` 被声明为 `volatile`，确保多线程环境下对 `connection` 的更改对所有线程可见。
3. **关闭连接方法**：提供了 `closeConnection()` 方法用于关闭连接，并在关闭后将 `connection` 置为 `null`，以便下次需要时可以重新初始化。
4. **私有构造函数**：`OracleConnectionUtil` 的构造函数为私有，防止外部实例化。

### 使用方式
- 在需要使用连接的地方调用 `OracleConnectionUtil.getConnection()` 来获取连接。
- 在不需要连接时，可以调用 `OracleConnectionUtil.closeConnection()` 关闭连接。

这个实现方式确保了 `Connection` 只会初始化一次，且线程安全，可以在多线程环境中可靠使用。
