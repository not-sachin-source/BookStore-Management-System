import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

// Database.java - opens the SQLite database file and creates the tables if they do not exist
public class Database {
    // The data is saved in the file bookstore.db (created in the folder where you run the program)
    private static final String URL = "jdbc:sqlite:bookstore.db";

    public static Connection connect() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");

            // Admin, vendors and customers all live in one table; "role" tells them apart
            st.execute("CREATE TABLE IF NOT EXISTS users ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "role TEXT NOT NULL, "
                    + "name TEXT NOT NULL, "
                    + "login_id TEXT NOT NULL UNIQUE COLLATE NOCASE, "
                    + "password TEXT NOT NULL, "
                    + "phone TEXT, "
                    + "city TEXT)");

            st.execute("CREATE TABLE IF NOT EXISTS books ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "title TEXT NOT NULL, "
                    + "author TEXT NOT NULL, "
                    + "category TEXT NOT NULL, "
                    + "price REAL NOT NULL, "
                    + "quantity INTEGER NOT NULL, "
                    + "vendor_id INTEGER NOT NULL REFERENCES users(id))");

            // Orders keep the customer and book names, so they stay readable even if a book is deleted later
            st.execute("CREATE TABLE IF NOT EXISTS orders ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "customer_id INTEGER NOT NULL REFERENCES users(id), "
                    + "customer_name TEXT NOT NULL, "
                    + "customer_email TEXT NOT NULL, "
                    + "book_title TEXT NOT NULL, "
                    + "vendor_name TEXT NOT NULL, "
                    + "quantity INTEGER NOT NULL, "
                    + "total REAL NOT NULL)");

            st.execute("CREATE TABLE IF NOT EXISTS activity_log ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "text TEXT NOT NULL)");
        }
        return conn;
    }
}
