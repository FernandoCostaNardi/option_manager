package com.olisystem.optionsmanager.service;

import com.olisystem.optionsmanager.model.User;
import com.olisystem.optionsmanager.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  public Optional<User> findByUsername(String username) {
    return userRepository.findByUsername(username);
  }
}
