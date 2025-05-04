package com.olisystem.optionsmanager.service.auth;

import com.olisystem.optionsmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    // Changed from findByUsername to findByEmail
    return userRepository
        .findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
  }
}
