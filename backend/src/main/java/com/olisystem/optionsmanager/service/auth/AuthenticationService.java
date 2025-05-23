package com.olisystem.optionsmanager.service.auth;

import com.olisystem.optionsmanager.config.jwt.JwtService;
import com.olisystem.optionsmanager.dto.auth.AuthenticationRequest;
import com.olisystem.optionsmanager.dto.auth.AuthenticationResponse;
import com.olisystem.optionsmanager.dto.auth.RegisterRequest;
import com.olisystem.optionsmanager.model.auth.Role;
import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
  private final UserRepository repository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;

  public AuthenticationResponse register(RegisterRequest request) {
    // colocar validacao de email e username
    if (repository.findByEmail(request.getEmail()).isPresent()) {
      throw new RuntimeException("Email já cadastrado");
    }
    if (repository.findByUsername(request.getUsername()).isPresent()) {
      throw new RuntimeException("Username já cadastrado");
    }
    var user =
        User.builder()
            .email(request.getEmail())
            .username(request.getUsername() != null ? request.getUsername() : request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(Role.USER)
            .build();

    repository.save(user);
    var jwtToken = jwtService.generateToken(user);
    return AuthenticationResponse.builder().token(jwtToken).build();
  }

  public AuthenticationResponse authenticate(AuthenticationRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
    var user = repository.findByEmail(request.getEmail()).orElseThrow();
    var jwtToken = jwtService.generateToken(user);
    return AuthenticationResponse.builder().token(jwtToken).build();
  }
}
