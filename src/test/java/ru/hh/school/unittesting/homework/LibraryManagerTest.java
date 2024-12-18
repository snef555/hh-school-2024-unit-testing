package ru.hh.school.unittesting.homework;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LibraryManagerTest {

  @Mock
  private NotificationService notificationService;

  @Mock
  private UserService userService;

  @InjectMocks
  private LibraryManager libraryManager;

  @BeforeEach
  void setUp() {
    libraryManager.addBook("availBookId1", 5);
    libraryManager.addBook("availBookId2", 3);
    libraryManager.addBook("availBookId3", 1);
  }

  @ParameterizedTest
  @CsvSource({
      "availBookId1, 5",
      "availBookId2, 3",
      "availBookId3, 1",
      "availBookId4, 0",
  })
  void testGetAvailableCopies(
      String bookId,
      int expectedAvailableCopies
  ) {
    int availableCopies = libraryManager.getAvailableCopies(bookId);

    assertEquals(expectedAvailableCopies, availableCopies);
  }

  @Test
  void borrowBookShouldReturnFalseAndSendOneNotifyIfUserNonActive() {
    String readerNonActive = "readerNonActive";
    String checkBorrowBookId = "availBookId1";

    when(userService.isUserActive(readerNonActive)).thenReturn(false);

    int availableCopies = libraryManager.getAvailableCopies(checkBorrowBookId);
    boolean borrowBookResult = libraryManager.borrowBook(checkBorrowBookId, readerNonActive);
    int availableCopiesAfterBorrowing = libraryManager.getAvailableCopies(checkBorrowBookId);

    verifyNoMoreInteractions(userService);
    verify(notificationService, times(1)).notifyUser(readerNonActive, "Your account is not active.");
    verifyNoMoreInteractions(notificationService);

    assertFalse(borrowBookResult);
    assertEquals(5, availableCopies);
    assertEquals(5, availableCopiesAfterBorrowing);
  }

  @Test
  void borrowBookShouldReturnFalseIfNotAvailableCopies() {
    String readerActive = "readerActive";
    String checkBorrowBookId = "checkBorrowBookId";

    when(userService.isUserActive(readerActive)).thenReturn(true);
    int availableCopies = libraryManager.getAvailableCopies(checkBorrowBookId);
    boolean borrowBookResult = libraryManager.borrowBook(checkBorrowBookId, readerActive);

    verifyNoMoreInteractions(userService);
    verifyNoInteractions(notificationService);

    assertFalse(borrowBookResult);
    assertEquals(0, availableCopies);
  }

  @Test
  void testBorrowBookSuccess() {
    String readerActive = "readerActive";
    String checkBorrowBookId = "availBookId1";

    when(userService.isUserActive(readerActive)).thenReturn(true);

    int availableCopies = libraryManager.getAvailableCopies(checkBorrowBookId);
    boolean borrowBookResult = libraryManager.borrowBook(checkBorrowBookId, readerActive);
    int availableCopiesAfterBorrowing = libraryManager.getAvailableCopies(checkBorrowBookId);

    verifyNoMoreInteractions(userService);
    verify(notificationService, times(1)).notifyUser(readerActive, "You have borrowed the book: " + checkBorrowBookId);
    verifyNoMoreInteractions(notificationService);

    assertTrue(borrowBookResult);
    assertEquals(5, availableCopies);
    assertEquals(4, availableCopiesAfterBorrowing);
  }

  @Test
  void testBorrowBookSameOneCopyForDifferentActiveUsers() {
    String readerActive1 = "readerActive1";
    String readerActive2 = "readerActive2";
    String checkBorrowBookId = "availBookId3";

    when(userService.isUserActive(readerActive1)).thenReturn(true);
    when(userService.isUserActive(readerActive2)).thenReturn(true);

    boolean borrowBookResultForReader1 = libraryManager.borrowBook(checkBorrowBookId, readerActive1);
    boolean borrowBookResultForReader2 = libraryManager.borrowBook(checkBorrowBookId, readerActive2);
    int availableCopies = libraryManager.getAvailableCopies(checkBorrowBookId);

    assertTrue(borrowBookResultForReader1);
    assertFalse(borrowBookResultForReader2);
    assertEquals(0, availableCopies);
  }

  @Test
  void returnBookShouldReturnFalseWhenBookIsNotBorrowed() {
    String checkReturnBookId = "availBookId1";

    int availableCopies = libraryManager.getAvailableCopies(checkReturnBookId);
    boolean returnBookResult = libraryManager.returnBook(checkReturnBookId, "readerActive1");
    int availableCopiesAfterReturning = libraryManager.getAvailableCopies(checkReturnBookId);

    assertFalse(returnBookResult);
    assertEquals(5, availableCopies);
    assertEquals(5, availableCopiesAfterReturning);
  }

  @Test
  void returnBookShouldReturnFalseWhenBookIsBorrowedOtherUser() {
    String readerBorrowBookActive = "readerActive1";
    String checkReturnBookId = "availBookId3";

    when(userService.isUserActive(readerBorrowBookActive)).thenReturn(true);

    libraryManager.borrowBook(checkReturnBookId, readerBorrowBookActive);
    int availableCopies = libraryManager.getAvailableCopies(checkReturnBookId);
    boolean returnBookResult = libraryManager.returnBook(checkReturnBookId, "readerActive2");

    assertFalse(returnBookResult);
    assertEquals(0, availableCopies);
  }

  @Test
  void testReturnBookSuccess() {
    String readerBorrowBookActive = "readerActive1";
    String checkReturnBookId = "availBookId2";

    when(userService.isUserActive(readerBorrowBookActive)).thenReturn(true);

    libraryManager.borrowBook(checkReturnBookId, readerBorrowBookActive);
    int availableCopies = libraryManager.getAvailableCopies(checkReturnBookId);
    boolean returnBookResult = libraryManager.returnBook(checkReturnBookId, readerBorrowBookActive);
    int availableCopiesAfterReturning = libraryManager.getAvailableCopies(checkReturnBookId);

    verify(notificationService, times(1)).notifyUser(readerBorrowBookActive, "You have returned the book: " + checkReturnBookId);

    assertTrue(returnBookResult);
    assertEquals(2, availableCopies);
    assertEquals(3, availableCopiesAfterReturning);
  }

  @Test
  void calculateDynamicLateFeeShouldThrowExceptionIfOverdueDaysNegative() {
    var exception = assertThrows(
        IllegalArgumentException.class,
        () -> libraryManager.calculateDynamicLateFee(-5, true, false)
    );
    assertEquals("Overdue days cannot be negative.", exception.getMessage());
  }

  @ParameterizedTest
  @CsvSource({
      "0, false, false, 0.0",
      "1, false, false, 0.5",
      "1, true, false, 0.75",
      "1, false, true, 0.4",
      "1, true, true, 0.6",
      "3, true, true, 1.8",
      "5, true, false, 3.75",
  })
  void testCalculateDynamicLateFee(
      int overdueDays,
      boolean isBestseller,
      boolean isPremiumMember,
      double expectedFee
  ) {
    double totalFee = libraryManager.calculateDynamicLateFee(overdueDays, isBestseller, isPremiumMember);

    assertEquals(expectedFee, totalFee);
  }

}
