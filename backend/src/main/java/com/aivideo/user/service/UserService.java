package com.aivideo.user.service;

import com.aivideo.user.entity.User;

public interface UserService {

    User findById(Long id);

    User findByUsernameOrEmail(String account);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    User create(User user);
}
