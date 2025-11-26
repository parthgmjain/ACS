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
        // Initialize using HTTP proxies to test full RPC stack
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
            // Setup
            Book book = new Book(TEST_ISBN_1, "Test Book", "Test Author", 10.0f, 5);
            stockManager.addBooks(Collections.singletonList(book));
            
            // Execute
            Map<Integer, Integer> ratings = new HashMap<>();
            ratings.put(TEST_ISBN_1, 4);
            bookStore.rateBooks(ratings);
            
            // Verify
            List<StockBook> storeBooks = stockManager.getBooks();
            assertEquals(1, storeBooks.size());
            StockBook ratedBook = storeBooks.get(0);
            assertEquals("Total rating should be 4", 4, ratedBook.getTotalRating());
            assertEquals("Should have 1 rating", 1, ratedBook.getNumRatings());
            assertEquals("Average should be 4.0", 4.0, ratedBook.getAverageRating(), 0.001);
            
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
            Book book = new Book(TEST_ISBN_1, "Test Book", "Test Author", 10.0f, 5);
            stockManager.addBooks(Collections.singletonList(book));
            
            // Rate the same book multiple times
            Map<Integer, Integer> firstRating = new HashMap<>();
            firstRating.put(TEST_ISBN_1, 3);
            bookStore.rateBooks(firstRating);
            
            Map<Integer, Integer> secondRating = new HashMap<>();
            secondRating.put(TEST_ISBN_1, 5);
            bookStore.rateBooks(secondRating);
            
            Map<Integer, Integer> thirdRating = new HashMap<>();
            thirdRating.put(TEST_ISBN_1, 4);
            bookStore.rateBooks(thirdRating);
            
            // Verify cumulative results
            List<StockBook> storeBooks = stockManager.getBooks();
            StockBook ratedBook = storeBooks.get(0);
            assertEquals("Total rating should be 12 (3+5+4)", 12, ratedBook.getTotalRating());
            assertEquals("Should have 3 ratings", 3, ratedBook.getNumRatings());
            assertEquals("Average should be 4.0", 4.0, ratedBook.getAverageRating(), 0.001);
            
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
            List<Book> books = Arrays.asList(
                new Book(TEST_ISBN_1, "Book 1", "Author 1", 10.0f, 5),
                new Book(TEST_ISBN_2, "Book 2", "Author 2", 15.0f, 5),
                new Book(TEST_ISBN_3, "Book 3", "Author 3", 20.0f, 5)
            );
            stockManager.addBooks(books);
            
            // Rate all books in one call
            Map<Integer, Integer> ratings = new HashMap<>();
            ratings.put(TEST_ISBN_1, 5);
            ratings.put(TEST_ISBN_2, 3);
            ratings.put(TEST_ISBN_3, 4);
            bookStore.rateBooks(ratings);
            
            // Verify all ratings were applied
            List<StockBook> storeBooks = stockManager.getBooks();
            Map<Integer, StockBook> bookMap = new HashMap<>();
            for (StockBook book : storeBooks) {
                bookMap.put(book.getISBN(), book);
            }
            
            assertEquals(5, bookMap.get(TEST_ISBN_1).getTotalRating());
            assertEquals(3, bookMap.get(TEST_ISBN_2).getTotalRating());
            assertEquals(4, bookMap.get(TEST_ISBN_3).getTotalRating());
            
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
            Book book = new Book(TEST_ISBN_1, "Test Book", "Test Author", 10.0f, 5);
            stockManager.addBooks(Collections.singletonList(book));
            
            Map<Integer, Integer> ratings = new HashMap<>();
            ratings.put(TEST_ISBN_1, 6); // Invalid - too high
            
            bookStore.rateBooks(ratings);
            fail("Should have thrown exception for rating > 5");
            
        } catch (BookStoreException e) {
            // Expected behavior
            assertTrue("Exception message should indicate invalid rating", 
                      e.getMessage().toLowerCase().contains("rating"));
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
            Book book = new Book(TEST_ISBN_1, "Test Book", "Test Author", 10.0f, 5);
            stockManager.addBooks(Collections.singletonList(book));
            
            Map<Integer, Integer> ratings = new HashMap<>();
            ratings.put(TEST_ISBN_1, -1); // Invalid - negative
            
            bookStore.rateBooks(ratings);
            fail("Should have thrown exception for negative rating");
            
        } catch (BookStoreException e) {
            // Expected behavior
            assertTrue("Exception message should indicate invalid rating",
                      e.getMessage().toLowerCase().contains("rating"));
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
            assertTrue("Exception message should indicate ISBN not found",
                      e.getMessage().toLowerCase().contains("isbn"));
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
            List<Book> books = Arrays.asList(
                new Book(TEST_ISBN_1, "Book 1", "Author 1", 10.0f, 5),
                new Book(TEST_ISBN_2, "Book 2", "Author 2", 15.0f, 5)
            );
            stockManager.addBooks(books);
            
            // Try to rate both books, but one rating is invalid
            Map<Integer, Integer> ratings = new HashMap<>();
            ratings.put(TEST_ISBN_1, 4);  // Valid
            ratings.put(TEST_ISBN_2, 6);  // Invalid - should cause rollback
            
            bookStore.rateBooks(ratings);
            fail("Should have thrown exception due to invalid rating");
            
        } catch (BookStoreException e) {
            // Verify that no ratings were applied (all-or-nothing)
            try {
                List<StockBook> storeBooks = stockManager.getBooks();
                for (StockBook book : storeBooks) {
                    assertEquals("No ratings should be applied after failure", 
                                0, book.getTotalRating());
                    assertEquals("No ratings should be applied after failure", 
                                0, book.getNumRatings());
                }
            } catch (Exception ex) {
                fail("Verification failed: " + ex.getMessage());
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
            Book book = new Book(TEST_ISBN_1, "Test Book", "Test Author", 10.0f, 5);
            stockManager.addBooks(Collections.singletonList(book));
            
            Map<Integer, Integer> ratings = new HashMap<>();
            ratings.put(TEST_ISBN_1, 0); // Minimum valid rating
            
            bookStore.rateBooks(ratings);
            
            // Verify rating was recorded
            List<StockBook> storeBooks = stockManager.getBooks();
            StockBook ratedBook = storeBooks.get(0);
            assertEquals(0, ratedBook.getTotalRating());
            assertEquals(1, ratedBook.getNumRatings());
            
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
        }
    }

    /**
     * Test edge case: rating of 5
     * Purpose: Verify maximum rating value is accepted
     */
    @Test
    public void testMaximumRating() {
        try {
            Book book = new Book(TEST_ISBN_1, "Test Book", "Test Author", 10.0f, 5);
            stockManager.addBooks(Collections.singletonList(book));
            
            Map<Integer, Integer> ratings = new HashMap<>();
            ratings.put(TEST_ISBN_1, 5); // Maximum valid rating
            
            bookStore.rateBooks(ratings);
            
            // Verify rating was recorded
            List<StockBook> storeBooks = stockManager.getBooks();
            StockBook ratedBook = storeBooks.get(0);
            assertEquals(5, ratedBook.getTotalRating());
            assertEquals(1, ratedBook.getNumRatings());
            
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
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
            // Should succeed without error for empty input
            
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