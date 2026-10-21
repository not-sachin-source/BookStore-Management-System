# Book Store Management System (Java Website)

A simple website with Admin, Vendor and Customer logins.
Plain Java only: no Tomcat, no Spring, no database.

## Logins

| Role | Login ID | Password |
|------|----------|----------|
| Admin | `admin` | `admin123` |
| Vendor (sample) | `vendor1` | `vendor123` |
| Vendor (sample) | `vendor2` | `vendor123` |
| Customer | Register on the home page, then log in with your email | your password |

## What Each Role Can Do

- **Admin:** add vendors; see every book and its stock with the vendor name; see the book and stock activity
  (who added which book or stock); see customer names and emails; see customer orders (which books, how many);
  delete books.
- **Vendor:** add books in a category; add stock to their own books; see their own stock.
- **Customer:** register, log in, search and filter books, choose a quantity and order, see their own orders.

## File Overview

| File | What it does |
|------|--------------|
| `User.java` | Abstract base class for all logins (name, login ID, private password). |
| `Admin.java` | Admin (extends User). |
| `Vendor.java` | Vendor (extends User) with phone and city. |
| `Customer.java` | Customer (extends User); the email is the login ID. |
| `Book.java` | Abstract base class for all books. |
| `Fiction.java`, `Crime.java`, `Horror.java`, `Comedy.java` | The four book categories (extend Book). |
| `Order.java` | One customer order: customer, book, quantity, total. |
| `Store.java` | All the logic: login, registration, books, stock, orders, activity log. |
| `StoreException.java` | Our own exception for store errors. |
| `WebServer.java` | The website: pages for each role and the buttons. Run this one. |

## Where Each OOP Concept Is Used

| Concept | Where |
|---------|-------|
| Classes & Objects | Store, User, Admin, Vendor, Customer, Book, Order and the four categories |
| Abstraction | `Book` and `User` are `abstract` |
| Inheritance | Admin, Vendor, Customer `extends User`; Fiction, Crime, Horror, Comedy `extends Book` |
| Encapsulation | Private fields with getters; the password is private and only checked with `checkPassword()` |
| Polymorphism | `getRole()` differs per user type; `getCategory()`, `getDiscountPercent()`, `getNote()` differ per book category |
| Exception Handling | `StoreException` thrown in `Store` (wrong login, bad details, not enough stock, wrong quantity), caught in `WebServer` and shown as a message |

## Step-by-Step: Run It

1. Install JDK 11 or newer. Check with: `javac -version`
2. Put all `.java` files in one folder.
3. Open a terminal in that folder.
4. Compile: `javac *.java`
5. Run: `java WebServer`
6. Open http://localhost:8080 in your browser.
7. Press Ctrl + C in the terminal to stop.

## Try This Flow

1. Register a customer (name, email, password), then log in.
2. Order a book and choose a quantity.
3. Log out and log in as `vendor1` / `vendor123`. Add a book and add stock.
4. Log out and log in as `admin` / `admin123`.
   You will see the new book and stock with the vendor name, the customer's name and email, and their order.

Data is kept in memory, so everything resets when the server stops.
Passwords are stored as plain text to keep the project simple.
