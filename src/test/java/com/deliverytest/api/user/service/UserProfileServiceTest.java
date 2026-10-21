package com.deliverytest.api.user.service;

import com.deliverytest.api.user.entity.User;
import com.deliverytest.api.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserProfileServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final UserProfileService profiles = new UserProfileService(users);

    @Test
    void returnsCurrentUserName() {
        when(users.findById("current")).thenReturn(Optional.of(User.builder().name("Maria").active(true).build()));
        assertThat(profiles.findCurrent("current").name()).isEqualTo("Maria");
    }

    @Test
    void rejectsMissingUser() {
        when(users.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> profiles.findCurrent("missing")).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void rejectsDisabledUser() {
        when(users.findById("disabled")).thenReturn(Optional.of(User.builder().active(false).build()));
        assertThatThrownBy(() -> profiles.findCurrent("disabled")).isInstanceOf(BadCredentialsException.class);
    }
}
