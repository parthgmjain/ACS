package com.acertainbookstore.client.tests;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.acertainbookstore.business.CertainBookStore;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.Book;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;

/**
 * Tests for getTopRatedBooks functionality
 * Purpose: Validate ranking algorithm, edge cases, and boundary conditions
 */
public class BookStoreTopRatedTest {

    private BookStore bookStore;
    private StockManager stockManager;
    private static boolean localTest = true;

    @Before
    public void setUp() throws Exception {
        try {
            String localTestProperty = System.getProperty(BookStoreConstants.PROPERTY_KEY_LOCAL_TEST);
            localTest = (localTestProperty != null) ? Boolean.parseBoolean(localTestProperty) : localTest;

            if (localTest) {
                CertainBookStore store = new CertainBookStore();
                stockManager = store;
                bookStore = store;
            } else {
                stockManager = new StockManagerHTTPProxy("http://localhost:8081/stock");
                bookStore = new BookStoreHTTPProxy("http://localhost:8081");
            }
            
            stockManager.removeAllBooks();
        } catch (BookStoreException e) {
            fail("Setup failed: " + e.getMessage());
        }
    }

    /**
     * Test k = 0
     * Purpose: Verify empty list is returned when k=0
     */
    @Test
    public void testZeroK() {
        try {
            Set<StockBook> books = new HashSet<>();
            books.add(new ImmutableStockBook(1, "Book 1", "Author 1", 10.0f, 5, 0, 0, 0, false));
            books.add(new ImmutableStockBook(2, "Book 2", "Author 2", 15.0f, 5, 0, 0, 0, false));
            stockManager.addBooks(books);
            
            List<Book> topRated = bookStore.getTopRatedBooks(0);
            
            assertNotNull("Should return empty list, not null", topRated);
            assertEquals("Should return empty list", 0, topRated.size());
            
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
            Set<StockBook> books = new HashSet<>();
            books.add(new ImmutableStockBook(1, "Book 1", "Author 1", 10.0f, 5, 0, 0, 0, false));
            books.add(new ImmutableStockBook(2, "Book 2", "Author 2", 15.0f, 5, 0, 0, 0, false));
            stockManager.addBooks(books);
            
            List<Book> topRated = bookStore.getTopRatedBooks(5);
            
            assertEquals("Should return all available books", 2, topRated.size());
            
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
        }
    }

    /**
     * Test books with no ratings
     * Purpose: Verify unrated books are handled correctly (average = 0)
     */
    @Test
    public void testTopRatedWithUnratedBooks() {
        try {
            Set<StockBook> books = new HashSet<>();
            books.add(new ImmutableStockBook(1, "Book 1", "Author 1", 10.0f, 5, 0, 0, 0, false));
            books.add(new ImmutableStockBook(2, "Book 2", "Author 2", 15.0f, 5, 0, 0, 0, false));
            books.add(new ImmutableStockBook(3, "Book 3", "Author 3", 20.0f, 5, 0, 0, 0, false));
            stockManager.addBooks(books);
            
            List<Book> topRated = bookStore.getTopRatedBooks(3);
            
            assertEquals(3, topRated.size());
            
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
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
            
            assertNotNull("Should return empty list, not null", topRated);
            assertEquals("Should return empty list", 0, topRated.size());
            
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
        }
    }
}