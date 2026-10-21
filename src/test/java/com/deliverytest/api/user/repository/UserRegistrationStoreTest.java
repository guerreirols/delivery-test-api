package com.deliverytest.api.user.repository;

import com.deliverytest.api.user.entity.User;
import com.deliverytest.api.user.exception.EmailAlreadyRegisteredException;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UserRegistrationStoreTest {
    private final UserRepository repository = mock(UserRepository.class);
    private final UserRegistrationStore store = new UserRegistrationStore(repository);
    private final User user = User.builder().name("Maria").email("maria@example.com").build();

    @Test
    void persistsUserWithImmediateConstraintValidation() {
        store.save(user);

        verify(repository).saveAndFlush(user);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void translatesEmailConstraintWithoutQueryingFailedTransaction() {
        DataIntegrityViolationException failure = failure("UNIQUE constraint failed: users.email");
        when(repository.saveAndFlush(user)).thenThrow(failure);

        assertThatThrownBy(() -> store.save(user)).isInstanceOf(EmailAlreadyRegisteredException.class).hasCause(failure);
        verify(repository).saveAndFlush(user);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void preservesOtherConstraintFailures() {
        DataIntegrityViolationException failure = failure("NOT NULL constraint failed: users.name");
        when(repository.saveAndFlush(user)).thenThrow(failure);

        assertThatThrownBy(() -> store.save(user)).isSameAs(failure);
    }

    @Test
    void handlesMissingDriverMessageWithoutHidingOriginalFailure() {
        DataIntegrityViolationException failure = failure(null);
        when(repository.saveAndFlush(user)).thenThrow(failure);

        assertThatThrownBy(() -> store.save(user)).isSameAs(failure);
    }

    private DataIntegrityViolationException failure(String message) {
        return new DataIntegrityViolationException("constraint", new SQLException(message));
    }
}
