package com.olisystem.optionsmanager.controller;

import com.olisystem.optionsmanager.dto.BrokerageCreateRequestDto;
import com.olisystem.optionsmanager.dto.BrokerageResponseDto;
import com.olisystem.optionsmanager.mapper.BrokerageMapper;
import com.olisystem.optionsmanager.model.Brokerage;
import com.olisystem.optionsmanager.model.User;
import com.olisystem.optionsmanager.service.BrokerageService;
import com.olisystem.optionsmanager.util.SecurityUtil;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/brokerages")
@RequiredArgsConstructor
public class BrokerageController {

  private final BrokerageService brokerageService;

  @GetMapping
  public ResponseEntity<Page<BrokerageResponseDto>> getAll(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String cnpj) {

    Pageable pageable = PageRequest.of(page, size);
    Page<Brokerage> brokerages = brokerageService.findAll(pageable, name, cnpj);
    Page<BrokerageResponseDto> dtoPage = brokerages.map(BrokerageMapper::toDto);
    return ResponseEntity.ok(dtoPage);
  }

  @GetMapping("/{id}")
  public ResponseEntity<BrokerageResponseDto> getById(@PathVariable UUID id) {
    return brokerageService
        .findById(id)
        .map(brokerage -> ResponseEntity.ok(BrokerageMapper.toDto(brokerage)))
        .orElse(ResponseEntity.notFound().build());
  }

  @PutMapping("/{id}")
  public ResponseEntity<BrokerageResponseDto> update(
      @PathVariable String id, @RequestBody BrokerageCreateRequestDto dto) {
    try {
      // Convert string ID to UUID, handling potential format issues
      UUID brokerageId;
      try {
        brokerageId = UUID.fromString(id);
      } catch (IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
      }

      return brokerageService
          .findById(brokerageId)
          .map(
              existing -> {
                // Get logged-in user
                User user = SecurityUtil.getLoggedUser();
                // Map DTO to entity and set ID
                Brokerage brokerageToUpdate = BrokerageMapper.toEntity(dto, user);
                brokerageToUpdate.setId(brokerageId);
                Brokerage updated = brokerageService.save(brokerageToUpdate);
                return ResponseEntity.ok(BrokerageMapper.toDto(updated));
              })
          .orElse(ResponseEntity.notFound().build());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PostMapping
  public ResponseEntity<BrokerageResponseDto> create(@RequestBody BrokerageCreateRequestDto dto) {
    User user = SecurityUtil.getLoggedUser();
    Brokerage brokerage = BrokerageMapper.toEntity(dto, user);

    return brokerageService
        .findByCnpj(brokerage.getCnpj())
        .map(
            existing ->
                ResponseEntity.status(HttpStatus.CONFLICT).body(BrokerageMapper.toDto(existing)))
        .orElseGet(
            () -> {
              Brokerage savedBrokerage = brokerageService.save(brokerage);
              return ResponseEntity.status(HttpStatus.CREATED)
                  .body(BrokerageMapper.toDto(savedBrokerage));
            });
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    try {
      if (brokerageService.findById(id).isPresent()) {
        brokerageService.deleteById(id);
        return ResponseEntity.noContent().build();
      } else {
        return ResponseEntity.notFound().build();
      }
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }
}
