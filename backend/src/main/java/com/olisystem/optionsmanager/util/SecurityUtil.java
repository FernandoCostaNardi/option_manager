package com.olisystem.optionsmanager.util;

import com.olisystem.optionsmanager.model.User;
import com.olisystem.optionsmanager.repository.UserRepository;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {

  private static UserRepository userRepository;

  @Autowired
  public SecurityUtil(UserRepository userRepository) {
    SecurityUtil.userRepository = userRepository;
  }

  /**
   * Attempts to retrieve the currently logged-in user.
   *
   * @return Optional containing the user if authenticated, empty otherwise
   */
  public static Optional<User> getLoggedUserOptional() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (isNotAuthenticated(authentication)) {
      return Optional.empty();
    }

    String email = authentication.getName();
    return userRepository.findByEmail(email);
  }

  /**
   * Gets the currently logged-in user or throws an exception if not found.
   *
   * @return The authenticated user
   * @throws IllegalStateException if no authenticated user is found
   */
  public static User getLoggedUser() {
    return getLoggedUserOptional()
        .orElseThrow(
            () -> new IllegalStateException("No authenticated user found. Please log in."));
  }

  private static boolean isNotAuthenticated(Authentication authentication) {
    return authentication == null || "anonymousUser".equals(authentication.getPrincipal());
  }
}
