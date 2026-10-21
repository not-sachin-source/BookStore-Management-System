import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// WebServer.java - starts the website, builds the pages, handles the buttons
public class WebServer {
    static Store store = new Store("City Book Store");
    static Map<String, User> sessions = new HashMap<>();   // who is logged in
    static String[] categories = {"Fiction", "Crime", "Horror", "Comedy"};

    public static void main(String[] args) throws IOException {
        // Sample data so the pages are not empty
        try {
            store.addVendor("City Books Supply", "vendor1", "vendor123", "9876543210", "Thrissur");
            store.addVendor("Readers Hub", "vendor2", "vendor123", "9123456780", "Kochi");
            Vendor v1 = store.getVendors().get(0);
            Vendor v2 = store.getVendors().get(1);
            store.addBook(v1, "Fiction", "The Lost Island", "A. Kumar", 300, 10);
            store.addBook(v2, "Crime", "Dark Alley", "R. Nair", 250, 6);
            store.addBook(v1, "Horror", "Midnight House", "S. Paul", 200, 4);
            store.addBook(v2, "Comedy", "Laugh Out Loud", "M. Das", 150, 8);
        } catch (StoreException e) {
            System.out.println("Sample data error: " + e.getMessage());
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", WebServer::home);
        server.createContext("/login", WebServer::login);
        server.createContext("/register", WebServer::register);
        server.createContext("/logout", WebServer::logout);
        server.createContext("/addvendor", WebServer::addVendor);
        server.createContext("/deletebook", WebServer::deleteBook);
        server.createContext("/addbook", WebServer::addBook);
        server.createContext("/addstock", WebServer::addStock);
        server.createContext("/order", WebServer::order);
        server.start();
        System.out.println("Bookstore running. Open http://localhost:8080 in your browser.");
    }

    // =====================================================
    //  PAGES
    // =====================================================

    // Shows a different page depending on who is logged in
    static void home(HttpExchange ex) throws IOException {
        User user = currentUser(ex);
        Map<String, String> q = parse(ex.getRequestURI().getRawQuery());
        String msg = q.getOrDefault("msg", "");

        StringBuilder h = new StringBuilder();
        startPage(h, msg, user);

        if (user == null) {
            loginPage(h);
        } else if (user instanceof Admin) {
            adminPage(h);
        } else if (user instanceof Vendor) {
            vendorPage(h, (Vendor) user);
        } else {
            customerPage(h, (Customer) user, q.getOrDefault("q", ""), q.getOrDefault("cat", ""));
        }

        h.append("</body></html>");
        byte[] data = h.toString().getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        ex.sendResponseHeaders(200, data.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(data);
        }
    }

    static void startPage(StringBuilder h, String msg, User user) {
        h.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        h.append("<title>").append(esc(store.getName())).append("</title><style>");
        h.append("body{font-family:Arial;max-width:1100px;margin:20px auto;padding:0 10px}");
        h.append("table{border-collapse:collapse;width:100%;margin-bottom:20px}");
        h.append("th,td{border:1px solid #ccc;padding:6px;text-align:left;font-size:14px}");
        h.append("th{background:#eee}");
        h.append("input,select{padding:5px;margin:3px}");
        h.append(".msg{background:#eef3ff;padding:8px;border:1px solid #99a}");
        h.append(".bar{background:#f3f3f3;padding:8px;margin-bottom:10px}");
        h.append("form.inline{display:inline}");
        h.append(".cols{display:flex;gap:20px;flex-wrap:wrap}");
        h.append(".box{border:1px solid #ccc;padding:12px;flex:1;min-width:280px}");
        h.append(".box input{display:block;width:90%}");
        h.append("</style></head><body>");
        h.append("<h1>").append(esc(store.getName())).append("</h1>");
        if (user != null) {
            h.append("<div class='bar'>Logged in as <b>").append(esc(user.getName()))
                    .append("</b> (").append(user.getRole()).append(") &nbsp; <a href='/logout'>Logout</a></div>");
        }
        if (!msg.isEmpty()) {
            h.append("<p class='msg'>").append(esc(msg)).append("</p>");
        }
    }

    // ---------- Login + customer registration ----------

    static void loginPage(StringBuilder h) {
        h.append("<div class='cols'>");

        h.append("<div class='box'><h3>Login</h3>");
        h.append("<form method='post' action='/login'>");
        h.append("<input name='loginId' placeholder='Login ID (or email for customers)' required>");
        h.append("<input name='password' type='password' placeholder='Password' required>");
        h.append("<button>Login</button></form>");
        h.append("<p>Admin, vendors and customers all log in here.</p></div>");

        h.append("<div class='box'><h3>New customer? Register</h3>");
        h.append("<form method='post' action='/register'>");
        h.append("<input name='name' placeholder='Your name' required>");
        h.append("<input name='email' type='email' placeholder='Email' required>");
        h.append("<input name='password' type='password' placeholder='Password (min 4 characters)' required>");
        h.append("<button>Register</button></form></div>");

        h.append("</div>");
    }

    // ---------- Admin dashboard ----------

    static void adminPage(StringBuilder h) {
        h.append("<h2>Admin Dashboard</h2>");

        h.append("<h3>Add a Vendor</h3>");
        h.append("<form method='post' action='/addvendor'>");
        h.append("<input name='name' placeholder='Vendor name' required>");
        h.append("<input name='loginId' placeholder='Login ID' required>");
        h.append("<input name='password' type='password' placeholder='Password' required>");
        h.append("<input name='phone' placeholder='Phone (digits)' required>");
        h.append("<input name='city' placeholder='City' required>");
        h.append("<button>Add Vendor</button></form>");

        h.append("<h3>Vendors</h3><table>");
        headRow(h, "Name", "Login ID", "Phone", "City", "Books Added");
        for (Vendor v : store.getVendors()) {
            h.append("<tr>");
            cell(h, v.getName());
            cell(h, v.getLoginId());
            cell(h, v.getPhone());
            cell(h, v.getCity());
            cell(h, store.booksOf(v).size());
            h.append("</tr>");
        }
        h.append("</table>");

        h.append("<h3>Books and Stock (with vendor)</h3><table>");
        headRow(h, "ID", "Title", "Author", "Category", "Vendor", "Price", "Stock", "Delete");
        for (Book b : store.search("", "")) {
            h.append("<tr>");
            cell(h, b.getId());
            cell(h, b.getTitle());
            cell(h, b.getAuthor());
            cell(h, b.getCategory());
            cell(h, b.getVendor().getName());
            cell(h, money(b.getPrice()));
            cell(h, b.getQuantity());
            h.append("<td><form class='inline' method='post' action='/deletebook'>");
            h.append("<input type='hidden' name='id' value='").append(b.getId()).append("'>");
            h.append("<button>Delete</button></form></td>");
            h.append("</tr>");
        }
        h.append("</table>");

        h.append("<h3>Book and Stock Activity</h3><table>");
        headRow(h, "What happened");
        for (String entry : store.getLog()) {
            h.append("<tr>");
            cell(h, entry);
            h.append("</tr>");
        }
        h.append("</table>");

        h.append("<h3>Customers</h3><table>");
        headRow(h, "Name", "Email");
        for (Customer c : store.getCustomers()) {
            h.append("<tr>");
            cell(h, c.getName());
            cell(h, c.getEmail());
            h.append("</tr>");
        }
        h.append("</table>");

        h.append("<h3>Customer Orders (books they want)</h3><table>");
        headRow(h, "Order", "Customer", "Email", "Book", "Quantity", "Total", "Vendor");
        for (Order o : store.getOrders()) {
            h.append("<tr>");
            cell(h, o.getId());
            cell(h, o.getCustomer().getName());
            cell(h, o.getCustomer().getEmail());
            cell(h, o.getBook().getTitle());
            cell(h, o.getQuantity());
            cell(h, money(o.getTotal()));
            cell(h, o.getBook().getVendor().getName());
            h.append("</tr>");
        }
        h.append("</table>");
    }

    // ---------- Vendor dashboard ----------

    static void vendorPage(StringBuilder h, Vendor v) {
        h.append("<h2>Vendor Dashboard</h2>");

        h.append("<h3>Add a Book</h3>");
        h.append("<form method='post' action='/addbook'>");
        h.append("<input name='title' placeholder='Title' required>");
        h.append("<input name='author' placeholder='Author' required>");
        h.append("<input name='price' type='number' step='0.01' placeholder='Price' required>");
        h.append("<input name='quantity' type='number' placeholder='Quantity' required>");
        h.append("<select name='category'>");
        for (String c : categories) {
            h.append("<option>").append(c).append("</option>");
        }
        h.append("</select>");
        h.append("<button>Add Book</button></form>");

        h.append("<h3>My Books and Stock</h3><table>");
        headRow(h, "ID", "Title", "Author", "Category", "Price", "Stock", "Add Stock");
        for (Book b : store.booksOf(v)) {
            h.append("<tr>");
            cell(h, b.getId());
            cell(h, b.getTitle());
            cell(h, b.getAuthor());
            cell(h, b.getCategory());
            cell(h, money(b.getPrice()));
            cell(h, b.getQuantity());
            h.append("<td><form class='inline' method='post' action='/addstock'>");
            h.append("<input type='hidden' name='id' value='").append(b.getId()).append("'>");
            h.append("<input type='number' name='qty' value='1' style='width:60px'>");
            h.append("<button>Add</button></form></td>");
            h.append("</tr>");
        }
        h.append("</table>");
    }

    // ---------- Customer page ----------

    static void customerPage(StringBuilder h, Customer c, String search, String cat) {
        h.append("<h2>Browse Books</h2>");

        h.append("<form method='get' action='/'>");
        h.append("<input name='q' placeholder='Title or author' value='").append(esc(search)).append("'>");
        h.append("<select name='cat'><option value=''>All categories</option>");
        for (String s : categories) {
            h.append("<option").append(s.equals(cat) ? " selected" : "").append(">").append(s).append("</option>");
        }
        h.append("</select>");
        h.append("<button>Search</button> <a href='/'>Show all</a></form>");

        h.append("<table>");
        headRow(h, "Title", "Author", "Category", "Price", "Discount", "Final Price", "Stock", "Note", "Order");
        for (Book b : store.search(search, cat)) {
            h.append("<tr>");
            cell(h, b.getTitle());
            cell(h, b.getAuthor());
            cell(h, b.getCategory());
            cell(h, money(b.getPrice()));
            cell(h, (int) b.getDiscountPercent() + "%");
            cell(h, money(b.getFinalPrice()));
            cell(h, b.getQuantity());
            cell(h, b.getNote());
            if (b.getQuantity() > 0) {
                h.append("<td><form class='inline' method='post' action='/order'>");
                h.append("<input type='hidden' name='id' value='").append(b.getId()).append("'>");
                h.append("<input type='number' name='qty' value='1' min='1' max='")
                        .append(b.getQuantity()).append("' style='width:60px'>");
                h.append("<button>Order</button></form></td>");
            } else {
                h.append("<td>Out of stock</td>");
            }
            h.append("</tr>");
        }
        h.append("</table>");

        h.append("<h3>My Orders</h3><table>");
        headRow(h, "Order", "Book", "Quantity", "Total");
        for (Order o : store.getOrdersOf(c)) {
            h.append("<tr>");
            cell(h, o.getId());
            cell(h, o.getBook().getTitle());
            cell(h, o.getQuantity());
            cell(h, money(o.getTotal()));
            h.append("</tr>");
        }
        h.append("</table>");
    }

    // =====================================================
    //  BUTTON ACTIONS (each one checks the role and handles its own errors)
    // =====================================================

    static void login(HttpExchange ex) throws IOException {
        if (!isPost(ex)) { redirect(ex, ""); return; }
        Map<String, String> f = parse(body(ex));
        try {
            User u = store.login(f.get("loginId"), f.get("password"));
            String sid = UUID.randomUUID().toString();
            sessions.put(sid, u);
            ex.getResponseHeaders().add("Set-Cookie", "sid=" + sid + "; Path=/; HttpOnly");
            redirect(ex, "Welcome, " + u.getName() + " (" + u.getRole() + ").");
        } catch (StoreException e) {
            redirect(ex, e.getMessage());
        }
    }

    static void register(HttpExchange ex) throws IOException {
        if (!isPost(ex)) { redirect(ex, ""); return; }
        Map<String, String> f = parse(body(ex));
        try {
            store.registerCustomer(f.get("name"), f.get("email"), f.get("password"));
            redirect(ex, "Registration successful. Please log in.");
        } catch (StoreException e) {
            redirect(ex, e.getMessage());
        }
    }

    static void logout(HttpExchange ex) throws IOException {
        String sid = getSid(ex);
        if (sid != null) {
            sessions.remove(sid);
        }
        ex.getResponseHeaders().add("Set-Cookie", "sid=; Path=/; Max-Age=0");
        redirect(ex, "Logged out.");
    }

    static void addVendor(HttpExchange ex) throws IOException {
        User u = currentUser(ex);
        if (!isPost(ex) || !(u instanceof Admin)) { redirect(ex, "Please log in as Admin."); return; }
        Map<String, String> f = parse(body(ex));
        try {
            store.addVendor(f.get("name"), f.get("loginId"), f.get("password"), f.get("phone"), f.get("city"));
            redirect(ex, "Vendor added.");
        } catch (StoreException e) {
            redirect(ex, e.getMessage());
        }
    }

    static void deleteBook(HttpExchange ex) throws IOException {
        User u = currentUser(ex);
        if (!isPost(ex) || !(u instanceof Admin)) { redirect(ex, "Please log in as Admin."); return; }
        Map<String, String> f = parse(body(ex));
        try {
            store.deleteBook(Integer.parseInt(f.get("id")));
            redirect(ex, "Book deleted.");
        } catch (StoreException e) {
            redirect(ex, e.getMessage());
        } catch (NumberFormatException | NullPointerException e) {
            redirect(ex, "Incorrect input. Invalid book ID.");
        }
    }

    static void addBook(HttpExchange ex) throws IOException {
        User u = currentUser(ex);
        if (!isPost(ex) || !(u instanceof Vendor)) { redirect(ex, "Please log in as Vendor."); return; }
        Map<String, String> f = parse(body(ex));
        try {
            double price = Double.parseDouble(f.get("price"));
            int qty = Integer.parseInt(f.get("quantity"));
            store.addBook((Vendor) u, f.get("category"), f.get("title"), f.get("author"), price, qty);
            redirect(ex, "Book added.");
        } catch (StoreException e) {
            redirect(ex, e.getMessage());
        } catch (NumberFormatException | NullPointerException e) {
            redirect(ex, "Incorrect input. Please check price and quantity.");
        }
    }

    static void addStock(HttpExchange ex) throws IOException {
        User u = currentUser(ex);
        if (!isPost(ex) || !(u instanceof Vendor)) { redirect(ex, "Please log in as Vendor."); return; }
        Map<String, String> f = parse(body(ex));
        try {
            store.addStock((Vendor) u, Integer.parseInt(f.get("id")), Integer.parseInt(f.get("qty")));
            redirect(ex, "Stock added.");
        } catch (StoreException e) {
            redirect(ex, e.getMessage());
        } catch (NumberFormatException | NullPointerException e) {
            redirect(ex, "Incorrect input. Please enter a valid quantity.");
        }
    }

    static void order(HttpExchange ex) throws IOException {
        User u = currentUser(ex);
        if (!isPost(ex) || !(u instanceof Customer)) { redirect(ex, "Please log in as Customer."); return; }
        Map<String, String> f = parse(body(ex));
        try {
            int id = Integer.parseInt(f.get("id"));
            int qty = Integer.parseInt(f.get("qty"));
            redirect(ex, store.placeOrder((Customer) u, id, qty));
        } catch (StoreException e) {
            redirect(ex, e.getMessage());
        } catch (NumberFormatException | NullPointerException e) {
            redirect(ex, "Incorrect input. Please enter a valid quantity.");
        }
    }

    // =====================================================
    //  SMALL HELPERS
    // =====================================================

    static boolean isPost(HttpExchange ex) {
        return ex.getRequestMethod().equals("POST");
    }

    // Finds the session id from the browser cookie
    static String getSid(HttpExchange ex) {
        String cookie = ex.getRequestHeaders().getFirst("Cookie");
        if (cookie == null) {
            return null;
        }
        for (String part : cookie.split(";")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length == 2 && kv[0].equals("sid")) {
                return kv[1];
            }
        }
        return null;
    }

    // Returns the logged-in user, or null if nobody is logged in
    static User currentUser(HttpExchange ex) {
        String sid = getSid(ex);
        if (sid == null) {
            return null;
        }
        return sessions.get(sid);
    }

    static String body(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    // Turns "a=1&b=2" into a Map
    static Map<String, String> parse(String s) throws IOException {
        Map<String, String> map = new HashMap<>();
        if (s == null || s.isEmpty()) {
            return map;
        }
        for (String pair : s.split("&")) {
            String[] kv = pair.split("=", 2);
            String key = URLDecoder.decode(kv[0], "UTF-8");
            String value = kv.length > 1 ? URLDecoder.decode(kv[1], "UTF-8") : "";
            map.put(key, value);
        }
        return map;
    }

    // Sends the browser back to the home page with a message
    static void redirect(HttpExchange ex, String msg) throws IOException {
        ex.getResponseHeaders().set("Location", "/?msg=" + URLEncoder.encode(msg, "UTF-8"));
        ex.sendResponseHeaders(303, -1);
        ex.close();
    }

    static void headRow(StringBuilder h, String... names) {
        h.append("<tr>");
        for (String n : names) {
            h.append("<th>").append(n).append("</th>");
        }
        h.append("</tr>");
    }

    static void cell(StringBuilder h, Object value) {
        h.append("<td>").append(esc(String.valueOf(value))).append("</td>");
    }

    static String money(double d) {
        return "Rs." + String.format("%.2f", d);
    }

    // Stops typed text from breaking the HTML
    static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("'", "&#39;").replace("\"", "&quot;");
    }
}
