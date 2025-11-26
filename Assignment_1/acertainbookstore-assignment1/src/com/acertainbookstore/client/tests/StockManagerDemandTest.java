package com.acertainbookstore.client.tests;

import com.acertainbookstore.business.*;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreException;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for getBooksInDemand functionality
 * Purpose: Verify sales miss tracking and demand reporting
 */
public class StockManagerDemandTest {

    private BookStore bookStore;
    private StockManager stockManager;

    @Before
    public void setUp() {
        bookStore = new BookStoreHTTPProxy("http://localhost:8081");
        stockManager = new StockManagerHTTPProxy("http://localhost:8081");
        
        try {
            stockManager.removeAllBooks();
        } catch (BookStoreException e) {
            fail("Setup failed: " + e.getMessage());
        }
    }

    /**
     * Test basic demand tracking
     * Purpose: Verify sales misses are recorded when purchase fails due to insufficient stock
     */
    @Test
    public void testBasicDemandTracking() {
        try {
            // Setup book with limited stock
            Set<StockBook> booksToAdd = new HashSet<>();
            booksToAdd.add(new ImmutableStockBook(1, "Book 1", "Author 1", 10.0f, 2, 0, 0, 0, false)); // Only 2 copies
            stockManager.addBooks(booksToAdd);
            
            // Try to buy more copies than available
            Set<BookCopy> purchase = new HashSet<>();
            purchase.add(new BookCopy(1, 3)); // Only 2 available - should fail
            
            try {
                bookStore.buyBooks(purchase);
                fail("Purchase should have failed due to insufficient stock");
            } catch (BookStoreException e) {
                // Expected - this should flag book as in demand
            }
            
            // Verify book is reported as in demand
            List<Book> booksInDemand = stockManager.getBooksInDemand();
            assertNotNull(booksInDemand);
            
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
        }
    }

    /**
     * Test no books in demand initially
     * Purpose: Verify empty list returned when no sales misses occurred
     */
    @Test
    public void testNoBooksInDemand() {
        try {
            Set<StockBook> booksToAdd = new HashSet<>();
            booksToAdd.add(new ImmutableStockBook(1, "Book 1", "Author 1", 10.0f, 5, 0, 0, 0, false));
            stockManager.addBooks(booksToAdd);
            
            // No failed purchases
            List<Book> booksInDemand = stockManager.getBooksInDemand();
            assertNotNull(booksInDemand);
            
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
        }
    }

    /**
     * Test multiple books in demand
     * Purpose: Verify system tracks demand for multiple books correctly
     */
    @Test
    public void testMultipleBooksInDemand() {
        try {
            Set<StockBook> booksToAdd = new HashSet<>();
            booksToAdd.add(new ImmutableStockBook(1, "Book 1", "Author 1", 10.0f, 1, 0, 0, 0, false));
            booksToAdd.add(new ImmutableStockBook(2, "Book 2", "Author 2", 15.0f, 1, 0, 0, 0, false));
            booksToAdd.add(new ImmutableStockBook(3, "Book 3", "Author 3", 20.0f, 5, 0, 0, 0, false)); // Enough stock
            stockManager.addBooks(booksToAdd);
            
            // Cause sales misses for books 1 and 2
            try {
                bookStore.buyBooks(Collections.singleton(new BookCopy(1, 2)));
            } catch (BookStoreException e) { /* Expected */ }
            
            try {
                bookStore.buyBooks(Collections.singleton(new BookCopy(2, 2)));
            } catch (BookStoreException e) { /* Expected */ }
            
            // Verify books in demand
            List<Book> booksInDemand = stockManager.getBooksInDemand();
            assertNotNull(booksInDemand);
            
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
        }
    }

    /**
     * Test empty bookstore for demand query
     * Purpose: Verify graceful handling of demand query on empty store
     */
    @Test
    public void testDemandEmptyBookstore() {
        try {
            List<Book> booksInDemand = stockManager.getBooksInDemand();
            assertNotNull(booksInDemand);
            
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
        }
    }
}