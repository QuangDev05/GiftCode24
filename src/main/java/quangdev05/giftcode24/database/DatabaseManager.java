package quangdev05.giftcode24.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public class DatabaseManager {
    private final JavaPlugin plugin;
    private HikariDataSource dataSource;
    private DatabaseType databaseType;
    private boolean isInitialConnection = true;

    public enum DatabaseType {
        H2, MYSQL
    }

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        String dbType = plugin.getConfig().getString("database.type", "H2").toUpperCase();
        try {
            this.databaseType = DatabaseType.valueOf(dbType);
        } catch (IllegalArgumentException e) {
            this.databaseType = DatabaseType.H2;
            plugin.getLogger().warning("Invalid database type in config, defaulting to H2");
        }

        try {
            HikariConfig config = new HikariConfig();

            switch (databaseType) {
                case H2:
                    File dbFile = new File(plugin.getDataFolder(), "playerdata");
                    String h2Url = "jdbc:h2:file:" + dbFile.getAbsolutePath() + ";MODE=MySQL";

                    config.setJdbcUrl(h2Url);
                    config.setDriverClassName("org.h2.Driver");
                    config.setUsername("sa");
                    config.setPassword("");

                    config.setMaximumPoolSize(10);
                    config.setMinimumIdle(2);
                    config.setConnectionTimeout(30000);
                    config.setIdleTimeout(300000);
                    config.setMaxLifetime(600000);
                    break;

                case MYSQL:
                    String host = plugin.getConfig().getString("database.mysql.host", "localhost");
                    int port = plugin.getConfig().getInt("database.mysql.port", 3306);
                    String database = plugin.getConfig().getString("database.mysql.database", "giftcode24");
                    String username = plugin.getConfig().getString("database.mysql.username", "root");
                    String password = plugin.getConfig().getString("database.mysql.password", "");

                    String mysqlUrl = "jdbc:mysql://" + host + ":" + port + "/" + database +
                            "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8";

                    config.setJdbcUrl(mysqlUrl);
                    config.setDriverClassName("com.mysql.cj.jdbc.Driver");
                    config.setUsername(username);
                    config.setPassword(password);

                    config.setMaximumPoolSize(15);
                    config.setMinimumIdle(5);
                    config.setConnectionTimeout(30000);
                    config.setIdleTimeout(300000);
                    config.setMaxLifetime(600000);
                    config.addDataSourceProperty("cachePrepStmts", "true");
                    config.addDataSourceProperty("prepStmtCacheSize", "250");
                    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                    config.addDataSourceProperty("useServerPrepStmts", "true");
                    config.addDataSourceProperty("useLocalSessionState", "true");
                    config.addDataSourceProperty("rewriteBatchedStatements", "true");
                    config.addDataSourceProperty("cacheResultSetMetadata", "true");
                    config.addDataSourceProperty("cacheServerConfiguration", "true");
                    config.addDataSourceProperty("elideSetAutoCommits", "true");
                    config.addDataSourceProperty("maintainTimeStats", "false");
                    break;
            }

            config.setPoolName("GiftCode24-HikariPool");
            config.setLeakDetectionThreshold(60000);
            config.setConnectionTestQuery("SELECT 1");

            this.dataSource = new HikariDataSource(config);

            testConnectionAndCreateTables();

            if (isInitialConnection) {
                plugin.getLogger().info("Connected to " + databaseType + " database using HikariCP successfully!");
                isInitialConnection = false;
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize HikariCP connection pool: " + e.getMessage());

            if (databaseType == DatabaseType.H2) {
                try {
                    plugin.getLogger().warning("Trying fallback to in-memory H2 database...");
                    HikariConfig fallbackConfig = new HikariConfig();
                    fallbackConfig.setJdbcUrl("jdbc:h2:mem:playerdata;DB_CLOSE_DELAY=-1;MODE=MySQL");
                    fallbackConfig.setDriverClassName("org.h2.Driver");
                    fallbackConfig.setUsername("sa");
                    fallbackConfig.setPassword("");
                    fallbackConfig.setMaximumPoolSize(5);

                    this.dataSource = new HikariDataSource(fallbackConfig);
                    testConnectionAndCreateTables();

                    if (isInitialConnection) {
                        plugin.getLogger().info("Connected to H2 database successfully!");
                        isInitialConnection = false;
                    }
                } catch (Exception ex) {
                    plugin.getLogger().severe("Failed to connect to H2 database: " + ex.getMessage());
                }
            }
        }
    }

    private void testConnectionAndCreateTables() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            String createPlayerDataTable = "CREATE TABLE IF NOT EXISTS player_data (" +
                    "uuid VARCHAR(36) PRIMARY KEY," +
                    "ip VARCHAR(45)," +
                    "used_codes TEXT," +
                    "assigned_codes TEXT" +
                    ");";

            stmt.execute(createPlayerDataTable);
        }
    }

    public Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get connection from HikariCP pool: " + e.getMessage());
            throw new RuntimeException("Database connection failed", e);
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("HikariCP connection pool closed");
        }
    }

    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    public void logPoolStats() {
        if (dataSource != null) {
            try {
                plugin.getLogger().info("HikariCP Pool Stats - " +
                        "Active: " + dataSource.getHikariPoolMXBean().getActiveConnections() +
                        ", Idle: " + dataSource.getHikariPoolMXBean().getIdleConnections() +
                        ", Total: " + dataSource.getHikariPoolMXBean().getTotalConnections() +
                        ", Waiting: " + dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
            } catch (Exception e) {
            }
        }
    }
}