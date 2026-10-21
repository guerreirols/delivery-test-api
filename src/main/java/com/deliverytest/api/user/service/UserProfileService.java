package com.deliverytest.api.user.service;

import com.deliverytest.api.user.dto.UserProfileResponse;
import com.deliverytest.api.user.entity.User;
import com.deliverytest.api.user.exception.InvalidUserSessionException;
import com.deliverytest.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    private final UserRepository users;

    public UserProfileResponse findCurrent(String userId) {
        User user = users.findById(userId).filter(User::isEnabled)
                .orElseThrow(InvalidUserSessionException::new);
        return new UserProfileResponse(user.getName());
    }
}
