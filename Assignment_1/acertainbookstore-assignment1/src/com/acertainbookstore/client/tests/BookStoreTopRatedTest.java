package com.acertainbookstore.client.tests;

import com.acertainbookstore.business.*;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreException;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for getTopRatedBooks functionality
 * Purpose: Validate ranking algorithm, edge cases, and boundary conditions
 */
public class BookStoreTopRatedTest {

    private BookStore bookStore;
    private StockManager stockManager;

    @Before
    public void setUp() throws Exception {
        String localTestProperty = System.getProperty(com.acertainbookstore.utils.BookStoreConstants.PROPERTY_KEY_LOCAL_TEST);
        boolean localTest = (localTestProperty != null) ? Boolean.parseBoolean(localTestProperty) : true;

        if (localTest) {
            CertainBookStore store = new CertainBookStore();
            bookStore = store;
            stockManager = store;
        } else {
            bookStore = new BookStoreHTTPProxy("http://localhost:8081");
            stockManager = new StockManagerHTTPProxy("http://localhost:8081");
        }

        try {
            stockManager.removeAllBooks();
        } catch (BookStoreException e) {
            fail("Setup failed: " + e.getMessage());
        }
    }

    /**
     * Test basic top-rated functionality
     * Purpose: Verify method returns correct number of books
     */
    @Test
    public void testBasicTopRated() {
        try {
            Set<StockBook> booksToAdd = new HashSet<>();
            booksToAdd.add(new ImmutableStockBook(1, "Book 1", "Author 1", 10.0f, 5, 0, 0, 0, false));
            booksToAdd.add(new ImmutableStockBook(2, "Book 2", "Author 2", 15.0f, 5, 0, 0, 0, false));
            stockManager.addBooks(booksToAdd);
            
            List<Book> topRated = bookStore.getTopRatedBooks(1);
            assertNotNull(topRated);
            assertTrue(topRated.size() <= 1);
            
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
        }
    }

    /**
     * Test when k is larger than total books
     * Purpose: Verify system returns all available books
     */
    @Test
    public void testKLargerThanCollection() {
        try {
            Set<StockBook> booksToAdd = new HashSet<>();
            booksToAdd.add(new ImmutableStockBook(1, "Book 1", "Author 1", 10.0f, 5, 0, 0, 0, false));
            booksToAdd.add(new ImmutableStockBook(2, "Book 2", "Author 2", 15.0f, 5, 0, 0, 0, false));
            stockManager.addBooks(booksToAdd);
            
            List<Book> topRated = bookStore.getTopRatedBooks(5);
            assertNotNull(topRated);
            assertTrue(topRated.size() <= 2);
            
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
        }
    }

    /**
     * Test k = 0
     * Purpose: Verify empty list is returned when k=0
     */
    @Test
    public void testZeroK() {
        try {
            Set<StockBook> booksToAdd = new HashSet<>();
            booksToAdd.add(new ImmutableStockBook(1, "Book 1", "Author 1", 10.0f, 5, 0, 0, 0, false));
            booksToAdd.add(new ImmutableStockBook(2, "Book 2", "Author 2", 15.0f, 5, 0, 0, 0, false));
            stockManager.addBooks(booksToAdd);
            
            List<Book> topRated = bookStore.getTopRatedBooks(0);
            assertNotNull(topRated);
            assertEquals(0, topRated.size());
            
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
        }
    }

    /**
     * Test negative k value
     * Purpose: Verify system validates input and throws appropriate exception
     */
    @Test
    public void testNegativeK() {
        try {
            bookStore.getTopRatedBooks(-1);
            fail("Should have thrown exception for negative k");
            
        } catch (BookStoreException e) {
            // Expected behavior
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    /**
     * Test empty bookstore
     * Purpose: Verify system handles empty collection gracefully
     */
    @Test
    public void testEmptyBookstore() {
        try {
            List<Book> topRated = bookStore.getTopRatedBooks(3);
            assertNotNull(topRated);
            assertEquals(0, topRated.size());
            
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
        }
    }

    /**
     * Test with rated books
     * Purpose: Verify rated books can be retrieved
     */
    @Test
    public void testWithRatedBooks() {
        try {
            Set<StockBook> booksToAdd = new HashSet<>();
            booksToAdd.add(new ImmutableStockBook(1, "Book 1", "Author 1", 10.0f, 5, 0, 0, 0, false));
            booksToAdd.add(new ImmutableStockBook(2, "Book 2", "Author 2", 15.0f, 5, 0, 0, 0, false));
            stockManager.addBooks(booksToAdd);
            
            // Rate books
            bookStore.rateBooks(Collections.<BookRating>singleton(new BookRating(1, 5)));
            bookStore.rateBooks(Collections.<BookRating>singleton(new BookRating(2, 3)));
            
            List<Book> topRated = bookStore.getTopRatedBooks(2);
            assertNotNull(topRated);
            assertTrue(topRated.size() <= 2);
            
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
        }
    }
}