package com.deliverytest.api.user.repository;

import com.deliverytest.api.user.entity.User;
import com.deliverytest.api.user.exception.EmailAlreadyRegisteredException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRegistrationStore {
    private static final String DUPLICATE_EMAIL_CONSTRAINT = "UNIQUE constraint failed: users.email";

    private final UserRepository userRepository;

    public void save(User user) {
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateEmail(exception)) {
                throw new EmailAlreadyRegisteredException(exception);
            }
            throw exception;
        }
    }

    private boolean isDuplicateEmail(DataIntegrityViolationException exception) {
        String message = exception.getMostSpecificCause().getMessage();
        return message != null && message.contains(DUPLICATE_EMAIL_CONSTRAINT);
    }
}
