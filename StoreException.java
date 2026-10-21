// StoreException.java - our own error type for problems in the bookstore
// (invalid details, wrong login, book not available, wrong quantity, etc.)
public class StoreException extends Exception {
    public StoreException(String message) {
        super(message);
    }
}
