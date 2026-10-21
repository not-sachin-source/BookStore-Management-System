// User.java - common base for Admin, Vendor and Customer
// Abstraction: you cannot create a plain User, only one of the three roles.
// Encapsulation: the password is private and can only be checked, never read.
public abstract class User {
    private String name;
    private String loginId;
    private String password;

    public User(String name, String loginId, String password) {
        this.name = name;
        this.loginId = loginId;
        this.password = password;
    }

    public String getName() { return name; }
    public String getLoginId() { return loginId; }

    public boolean checkPassword(String input) {
        return password.equals(input);
    }

    // Polymorphism: each role gives its own answer
    public abstract String getRole();
}
