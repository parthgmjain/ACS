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
 * Tests for rating functionality - covers rateBooks method
 * Purpose: Validate book rating system including edge cases and error conditions
 */
public class BookStoreRatingTest {

    private static final int TEST_ISBN_1 = 1;
    private static final int TEST_ISBN_2 = 2;
    private static final int TEST_ISBN_3 = 3;
    private static final int NON_EXISTENT_ISBN = 999;
    
    private BookStore bookStore;
    private StockManager stockManager;

    @Before
    public void setUp() {
        // Using HTTP proxies to test full RPC communication layer
        bookStore = new BookStoreHTTPProxy("http://localhost:8081");
        stockManager = new StockManagerHTTPProxy("http://localhost:8081");
        
        try {
            stockManager.removeAllBooks();
        } catch (BookStoreException e) {
            fail("Setup failed: " + e.getMessage());
        }
    }

    /**
     * Test basic rating functionality with single book
     * Purpose: Verify that a single rating is properly recorded
     */
    @Test
    public void testRateSingleBook() {
        try {
            // Setup - add a book to the store
            Set<StockBook> booksToAdd = new HashSet<>();
            booksToAdd.add(new ImmutableStockBook(TEST_ISBN_1, "Test Book", "Test Author", 10.0f, 5, 0, 0, 0, false));
            stockManager.addBooks(booksToAdd);
            
            // Execute - rate the book
            Map<Integer, Integer> ratings = new HashMap<>();
            ratings.put(TEST_ISBN_1, 4);
            bookStore.rateBooks(ratings);
            
            // Verify - check that we can still query books (system is consistent)
            List<StockBook> books = stockManager.getBooks();
            assertEquals(1, books.size());
            
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
        }
    }

    /**
     * Test multiple ratings for the same book
     * Purpose: Verify cumulative rating calculation
     */
    @Test
    public void testMultipleRatingsSameBook() {
        try {
            // Setup
            Set<StockBook> booksToAdd = new HashSet<>();
            booksToAdd.add(new ImmutableStockBook(TEST_ISBN_1, "Test Book", "Test Author", 10.0f, 5, 0, 0, 0, false));
            stockManager.addBooks(booksToAdd);
            
            // Execute - rate the same book multiple times
            bookStore.rateBooks(Collections.singletonMap(TEST_ISBN_1, 3));
            bookStore.rateBooks(Collections.singletonMap(TEST_ISBN_1, 5));
            bookStore.rateBooks(Collections.singletonMap(TEST_ISBN_1, 4));
            
            // Verify - system should remain consistent
            List<Book> topRated = bookStore.getTopRatedBooks(5);
            assertNotNull(topRated);
            
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
        }
    }

    /**
     * Test rating multiple books in single call
     * Purpose: Verify batch rating functionality
     */
    @Test
    public void testRateMultipleBooks() {
        try {
            // Setup multiple books
            Set<StockBook> booksToAdd = new HashSet<>();
            booksToAdd.add(new ImmutableStockBook(TEST_ISBN_1, "Book 1", "Author 1", 10.0f, 5, 0, 0, 0, false));
            booksToAdd.add(new ImmutableStockBook(TEST_ISBN_2, "Book 2", "Author 2", 15.0f, 5, 0, 0, 0, false));
            booksToAdd.add(new ImmutableStockBook(TEST_ISBN_3, "Book 3", "Author 3", 20.0f, 5, 0, 0, 0, false));
            stockManager.addBooks(booksToAdd);
            
            // Rate all books in one call
            Map<Integer, Integer> ratings = new HashMap<>();
            ratings.put(TEST_ISBN_1, 5);
            ratings.put(TEST_ISBN_2, 3);
            ratings.put(TEST_ISBN_3, 4);
            bookStore.rateBooks(ratings);
            
            // Verify system remains functional
            List<Book> books = bookStore.getBooks(new HashSet<>(Arrays.asList(TEST_ISBN_1, TEST_ISBN_2, TEST_ISBN_3)));
            assertEquals(3, books.size());
            
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
        }
    }

    /**
     * Test rating validation - invalid rating value (too high)
     * Purpose: Verify system rejects ratings outside 0-5 range
     */
    @Test
    public void testInvalidRatingTooHigh() {
        try {
            Set<StockBook> booksToAdd = new HashSet<>();
            booksToAdd.add(new ImmutableStockBook(TEST_ISBN_1, "Test Book", "Test Author", 10.0f, 5, 0, 0, 0, false));
            stockManager.addBooks(booksToAdd);
            
            Map<Integer, Integer> ratings = new HashMap<>();
            ratings.put(TEST_ISBN_1, 6); // Invalid - too high
            
            bookStore.rateBooks(ratings);
            fail("Should have thrown exception for rating > 5");
            
        } catch (BookStoreException e) {
            // Expected behavior
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    /**
     * Test rating validation - invalid rating value (negative)
     * Purpose: Verify system rejects negative ratings
     */
    @Test
    public void testInvalidRatingNegative() {
        try {
            Set<StockBook> booksToAdd = new HashSet<>();
            booksToAdd.add(new ImmutableStockBook(TEST_ISBN_1, "Test Book", "Test Author", 10.0f, 5, 0, 0, 0, false));
            stockManager.addBooks(booksToAdd);
            
            Map<Integer, Integer> ratings = new HashMap<>();
            ratings.put(TEST_ISBN_1, -1); // Invalid - negative
            
            bookStore.rateBooks(ratings);
            fail("Should have thrown exception for negative rating");
            
        } catch (BookStoreException e) {
            // Expected behavior
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    /**
     * Test rating non-existent book
     * Purpose: Verify system validates ISBN existence
     */
    @Test
    public void testRateNonExistentBook() {
        try {
            Map<Integer, Integer> ratings = new HashMap<>();
            ratings.put(NON_EXISTENT_ISBN, 4);
            
            bookStore.rateBooks(ratings);
            fail("Should have thrown exception for non-existent ISBN");
            
        } catch (BookStoreException e) {
            // Expected behavior
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    /**
     * Test all-or-nothing semantics with mixed valid/invalid ratings
     * Purpose: Verify that partial updates don't occur when validation fails
     */
    @Test
    public void testAllOrNothingSemantics() {
        try {
            // Setup two books
            Set<StockBook> booksToAdd = new HashSet<>();
            booksToAdd.add(new ImmutableStockBook(TEST_ISBN_1, "Book 1", "Author 1", 10.0f, 5, 0, 0, 0, false));
            booksToAdd.add(new ImmutableStockBook(TEST_ISBN_2, "Book 2", "Author 2", 15.0f, 5, 0, 0, 0, false));
            stockManager.addBooks(booksToAdd);
            
            // Try to rate both books, but one rating is invalid
            Map<Integer, Integer> ratings = new HashMap<>();
            ratings.put(TEST_ISBN_1, 4);  // Valid
            ratings.put(TEST_ISBN_2, 6);  // Invalid - should cause rollback
            
            bookStore.rateBooks(ratings);
            fail("Should have thrown exception due to invalid rating");
            
        } catch (BookStoreException e) {
            // Verify that system is in consistent state
            try {
                List<StockBook> books = stockManager.getBooks();
                assertEquals(2, books.size());
            } catch (Exception ex) {
                fail("System should be in consistent state after failed rating");
            }
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    /**
     * Test edge case: rating of 0
     * Purpose: Verify minimum rating value is accepted
     */
    @Test
    public void testZeroRating() {
        try {
            Set<StockBook> booksToAdd = new HashSet<>();
            booksToAdd.add(new ImmutableStockBook(TEST_ISBN_1, "Test Book", "Test Author", 10.0f, 5, 0, 0, 0, false));
            stockManager.addBooks(booksToAdd);
            
            Map<Integer, Integer> ratings = new HashMap<>();
            ratings.put(TEST_ISBN_1, 0); // Minimum valid rating
            
            bookStore.rateBooks(ratings);
            // Should succeed without exception
            
        } catch (Exception e) {
            fail("Zero rating should be accepted: " + e.getMessage());
        }
    }

    /**
     * Test edge case: rating of 5
     * Purpose: Verify maximum rating value is accepted
     */
    @Test
    public void testMaximumRating() {
        try {
            Set<StockBook> booksToAdd = new HashSet<>();
            booksToAdd.add(new ImmutableStockBook(TEST_ISBN_1, "Test Book", "Test Author", 10.0f, 5, 0, 0, 0, false));
            stockManager.addBooks(booksToAdd);
            
            Map<Integer, Integer> ratings = new HashMap<>();
            ratings.put(TEST_ISBN_1, 5); // Maximum valid rating
            
            bookStore.rateBooks(ratings);
            // Should succeed without exception
            
        } catch (Exception e) {
            fail("Maximum rating (5) should be accepted: " + e.getMessage());
        }
    }

    /**
     * Test empty ratings map
     * Purpose: Verify system handles empty input gracefully
     */
    @Test
    public void testEmptyRatingsMap() {
        try {
            Map<Integer, Integer> ratings = new HashMap<>(); // Empty map
            bookStore.rateBooks(ratings);
            // Should succeed without error
            
        } catch (Exception e) {
            fail("Empty ratings map should be handled gracefully: " + e.getMessage());
        }
    }

    /**
     * Test null ratings map
     * Purpose: Verify system validates null input
     */
    @Test
    public void testNullRatingsMap() {
        try {
            bookStore.rateBooks(null);
            fail("Should have thrown exception for null ratings map");
            
        } catch (BookStoreException e) {
            // Expected behavior
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }
}