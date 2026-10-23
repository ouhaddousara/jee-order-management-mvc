package ma.fstt.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;

/**
 * Single entry point to obtain JDBC connections. One instance for the whole
 * application ({@code @ApplicationScoped}), injected into the DAO classes with {@code @Inject}.
 *
 * <p>Configuration is read once, lazily, from {@code db.properties} on the classpath
 * (keys {@code db.url}, {@code db.user}, {@code db.password}). The environment variables
 * {@code DB_URL}, {@code DB_USER} and {@code DB_PASSWORD} take precedence, so credentials
 * never have to be committed to Git.
 *
 * <p>Each call returns a new connection that the caller must close (try-with-resources).
 * A connection pool (Tomcat JNDI DataSource) would be the next step for production;
 * for this workshop {@link DriverManager} keeps the moving parts to a minimum.
 */
@ApplicationScoped
public class DBConnection {

    static final String CONFIG_FILE = "db.properties";
    private static final String DEFAULT_DRIVER = "com.mysql.cj.jdbc.Driver";

    private String url;
    private String user;
    private String password;
    private boolean configured;

    /** Used by CDI: the configuration is loaded on first use. */
    public DBConnection() {
    }

    /** Explicit configuration, handy for tests and command-line tools. */
    public DBConnection(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
        this.configured = true;
    }

    public Connection getConnection() throws SQLException {
        ensureConfigured();
        return DriverManager.getConnection(url, user, password);
    }

    private synchronized void ensureConfigured() {
        if (configured) {
            return;
        }
        Properties props = loadProperties();
        url = firstNonBlank(System.getenv("DB_URL"), props.getProperty("db.url"));
        user = firstNonBlank(System.getenv("DB_USER"), props.getProperty("db.user"));
        password = firstNonBlank(System.getenv("DB_PASSWORD"), props.getProperty("db.password"));
        if (url == null || user == null) {
            throw new IllegalStateException("Database configuration not found. Copy "
                    + "src/main/resources/db.properties.example to db.properties and fill in your credentials.");
        }
        try {
            // Registers the driver with the web application's class loader (needed on Tomcat).
            Class.forName(props.getProperty("db.driver", DEFAULT_DRIVER));
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("JDBC driver not found on the classpath", e);
        }
        configured = true;
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = DBConnection.class.getClassLoader();
        }
        try (InputStream in = loader.getResourceAsStream(CONFIG_FILE)) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + CONFIG_FILE, e);
        }
        return props;
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return (second == null || second.isBlank()) ? null : second;
    }
}
