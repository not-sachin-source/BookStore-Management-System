import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

// Store.java - connects the Store, its Users, Books and Orders. All the logic is here.
public class Store {
    private String name;
    private Admin admin = new Admin("Administrator", "admin", "admin123");
    private ArrayList<Vendor> vendors = new ArrayList<>();
    private ArrayList<Customer> customers = new ArrayList<>();
    private ArrayList<Book> books = new ArrayList<>();
    private ArrayList<Order> orders = new ArrayList<>();
    private ArrayList<String> log = new ArrayList<>();
    private int nextBookId = 1;
    private int nextOrderId = 1;

    public Store(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public ArrayList<Vendor> getVendors() { return vendors; }
    public ArrayList<Customer> getCustomers() { return customers; }
    public ArrayList<Order> getOrders() { return orders; }
    public ArrayList<String> getLog() { return log; }

    // ---------- Login and registration ----------

    public User login(String loginId, String password) throws StoreException {
        if (loginId == null || loginId.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new StoreException("Enter your login ID and password.");
        }
        loginId = loginId.trim();
        // Polymorphism: Admin, Vendor and Customer are all treated as User here
        ArrayList<User> all = new ArrayList<>();
        all.add(admin);
        all.addAll(vendors);
        all.addAll(customers);
        for (User u : all) {
            if (u.getLoginId().equalsIgnoreCase(loginId) && u.checkPassword(password)) {
                return u;
            }
        }
        throw new StoreException("Invalid login ID or password.");
    }

    public void registerCustomer(String name, String email, String password) throws StoreException {
        if (name == null || name.trim().isEmpty() || email == null || email.trim().isEmpty()
                || password == null) {
            throw new StoreException("Name, email and password are required.");
        }
        email = email.trim().toLowerCase();
        if (!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new StoreException("Enter a valid email address.");
        }
        if (password.length() < 4) {
            throw new StoreException("Password must have at least 4 characters.");
        }
        if (loginIdTaken(email)) {
            throw new StoreException("This email is already registered.");
        }
        customers.add(new Customer(name.trim(), email, password));
    }

    // Admin creates vendors
    public void addVendor(String name, String loginId, String password,
                          String phone, String city) throws StoreException {
        if (name == null || name.trim().isEmpty() || loginId == null || loginId.trim().isEmpty()
                || city == null || city.trim().isEmpty() || password == null) {
            throw new StoreException("Vendor name, login ID, password and city are required.");
        }
        if (password.length() < 4) {
            throw new StoreException("Password must have at least 4 characters.");
        }
        if (phone == null || !phone.trim().matches("\\d{7,15}")) {
            throw new StoreException("Phone must contain only digits (7 to 15).");
        }
        if (loginIdTaken(loginId.trim())) {
            throw new StoreException("This login ID is already used.");
        }
        vendors.add(new Vendor(name.trim(), loginId.trim(), password, phone.trim(), city.trim()));
    }

    // ---------- Books ----------

    public void addBook(Vendor vendor, String category, String title, String author,
                        double price, int quantity) throws StoreException {
        if (title == null || title.trim().isEmpty() || author == null || author.trim().isEmpty()) {
            throw new StoreException("Title and author cannot be empty.");
        }
        if (price <= 0) {
            throw new StoreException("Price must be greater than 0.");
        }
        if (quantity < 0) {
            throw new StoreException("Quantity cannot be negative.");
        }
        if (category == null) {
            throw new StoreException("Please choose a category.");
        }

        title = title.trim();
        author = author.trim();
        Book b;
        switch (category) {
            case "Fiction": b = new Fiction(nextBookId, title, author, price, quantity, vendor); break;
            case "Crime":   b = new Crime(nextBookId, title, author, price, quantity, vendor); break;
            case "Horror":  b = new Horror(nextBookId, title, author, price, quantity, vendor); break;
            case "Comedy":  b = new Comedy(nextBookId, title, author, price, quantity, vendor); break;
            default: throw new StoreException("Unknown category: " + category);
        }
        books.add(b);
        nextBookId++;
        addLog("Vendor " + vendor.getName() + " added book '" + title + "' (" + category
                + ") with stock " + quantity + ".");
    }

    // A vendor can add stock only to their own books
    public void addStock(Vendor vendor, int bookId, int qty) throws StoreException {
        Book b = findBook(bookId);
        if (b == null || b.getVendor() != vendor) {
            throw new StoreException("You can only add stock to your own books.");
        }
        if (qty <= 0) {
            throw new StoreException("Stock quantity must be at least 1.");
        }
        b.addStock(qty);
        addLog("Vendor " + vendor.getName() + " added " + qty + " stock to '" + b.getTitle()
                + "'. Stock is now " + b.getQuantity() + ".");
    }

    public void deleteBook(int id) throws StoreException {
        Book b = findBook(id);
        if (b == null) {
            throw new StoreException("Book not found.");
        }
        books.remove(b);
        addLog("Admin deleted book '" + b.getTitle() + "' (vendor " + b.getVendor().getName() + ").");
    }

    // Empty keyword / empty category means "match all"
    public ArrayList<Book> search(String keyword, String category) {
        ArrayList<Book> result = new ArrayList<>();
        String k = keyword.toLowerCase();
        for (Book b : books) {
            boolean textMatch = b.getTitle().toLowerCase().contains(k)
                    || b.getAuthor().toLowerCase().contains(k);
            boolean categoryMatch = category.isEmpty() || b.getCategory().equals(category);
            if (textMatch && categoryMatch) {
                result.add(b);
            }
        }
        return result;
    }

    public ArrayList<Book> booksOf(Vendor vendor) {
        ArrayList<Book> result = new ArrayList<>();
        for (Book b : books) {
            if (b.getVendor() == vendor) {
                result.add(b);
            }
        }
        return result;
    }

    // ---------- Orders ----------

    public String placeOrder(Customer customer, int bookId, int qty) throws StoreException {
        Book b = findBook(bookId);
        if (b == null) {
            throw new StoreException("Book not available.");
        }
        if (qty <= 0) {
            throw new StoreException("Order quantity must be at least 1.");
        }
        if (qty > b.getQuantity()) {
            throw new StoreException("Only " + b.getQuantity() + " copies of '" + b.getTitle() + "' in stock.");
        }
        b.reduceStock(qty);
        double total = b.getFinalPrice() * qty;
        orders.add(new Order(nextOrderId, customer, b, qty, total));
        nextOrderId++;
        return "Order placed: " + qty + " x " + b.getTitle() + ". Total: Rs." + String.format("%.2f", total);
    }

    public ArrayList<Order> getOrdersOf(Customer customer) {
        ArrayList<Order> result = new ArrayList<>();
        for (Order o : orders) {
            if (o.getCustomer() == customer) {
                result.add(o);
            }
        }
        return result;
    }

    // ---------- Private helpers ----------

    private Book findBook(int id) {
        for (Book b : books) {
            if (b.getId() == id) {
                return b;
            }
        }
        return null;
    }

    private boolean loginIdTaken(String loginId) {
        if (admin.getLoginId().equalsIgnoreCase(loginId)) {
            return true;
        }
        for (Vendor v : vendors) {
            if (v.getLoginId().equalsIgnoreCase(loginId)) {
                return true;
            }
        }
        for (Customer c : customers) {
            if (c.getLoginId().equalsIgnoreCase(loginId)) {
                return true;
            }
        }
        return false;
    }

    // Newest entry goes first
    private void addLog(String text) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
        log.add(0, time + "  -  " + text);
    }
}
