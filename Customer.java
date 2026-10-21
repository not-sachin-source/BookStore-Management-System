// Customer.java - a buyer who registered on the website (inherits from User)
// The customer's email is used as the login ID.
public class Customer extends User {
    public Customer(String name, String email, String password) {
        super(name, email, password);
    }

    public String getEmail() { return getLoginId(); }

    @Override
    public String getRole() { return "Customer"; }
}
