package com.jpmc;

import com.jpmc.midascore.component.TransactionListener;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TransactionListenerUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private TransactionListener transactionListener;

    private UserRecord sender;
    private UserRecord recipient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        sender = new UserRecord("Alice", 500.0f);
        recipient = new UserRecord("Bob", 100.0f);
    }

    @Test
    void validTransaction_shouldUpdateBothBalances() {
        // Arrange
        Transaction transaction = new Transaction(1, 2, 100.0f);
        when(userRepository.findById(1L)).thenReturn(sender);
        when(userRepository.findById(2L)).thenReturn(recipient);

        // Act
        transactionListener.listen(transaction);

        // Assert
        verify(userRepository, times(2)).save(any(UserRecord.class));
    }

    @Test
    void insufficientFunds_shouldNotSaveAnything() {
        // Arrange — sender only has £50 but tries to send £200
        sender.setBalance(50.0f);
        Transaction transaction = new Transaction(1, 2, 200.0f);
        when(userRepository.findById(1L)).thenReturn(sender);
        when(userRepository.findById(2L)).thenReturn(recipient);

        // Act
        transactionListener.listen(transaction);

        // Assert — save should never be called
        verify(userRepository, never()).save(any(UserRecord.class));
    }

    @Test
    void senderNotFound_shouldNotSaveAnything() {
        // Arrange — sender doesn't exist in DB
        Transaction transaction = new Transaction(99, 2, 100.0f);
        when(userRepository.findById(99L)).thenReturn(null);
        when(userRepository.findById(2L)).thenReturn(recipient);

        // Act
        transactionListener.listen(transaction);

        // Assert
        verify(userRepository, never()).save(any(UserRecord.class));
    }

    @Test
    void recipientNotFound_shouldNotSaveAnything() {
        // Arrange — recipient doesn't exist in DB
        Transaction transaction = new Transaction(1, 99, 100.0f);
        when(userRepository.findById(1L)).thenReturn(sender);
        when(userRepository.findById(99L)).thenReturn(null);

        // Act
        transactionListener.listen(transaction);

        // Assert
        verify(userRepository, never()).save(any(UserRecord.class));
    }

    @Test
    void exactBalance_shouldProcessSuccessfully() {
        // Arrange — sender sends exactly their entire balance
        sender.setBalance(100.0f);
        Transaction transaction = new Transaction(1, 2, 100.0f);
        when(userRepository.findById(1L)).thenReturn(sender);
        when(userRepository.findById(2L)).thenReturn(recipient);

        // Act
        transactionListener.listen(transaction);

        // Assert — should succeed
        verify(userRepository, times(2)).save(any(UserRecord.class));
    }
}