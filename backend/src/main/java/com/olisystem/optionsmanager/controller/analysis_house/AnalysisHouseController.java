package com.olisystem.optionsmanager.controller.analysis_house;

import com.olisystem.optionsmanager.dto.analysis_house.AnalysisHouseCreateRequestDto;
import com.olisystem.optionsmanager.dto.analysis_house.AnalysisHouseResponseDto;
import com.olisystem.optionsmanager.mapper.analysis_house.AnalysisHouseMapper;
import com.olisystem.optionsmanager.model.analysis_house.AnalysisHouse;
import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.service.analysis_house.AnalysisHouseService;
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
@RequestMapping("/api/analysis-houses")
@RequiredArgsConstructor
public class AnalysisHouseController {

  private final AnalysisHouseService analysisHouseService;

  @GetMapping
  public ResponseEntity<Page<AnalysisHouseResponseDto>> getAll(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String name) {

    Pageable pageable = PageRequest.of(page, size);
    Page<AnalysisHouse> analysisHouses = analysisHouseService.findAll(pageable, name);
    Page<AnalysisHouseResponseDto> dtoPage = analysisHouses.map(AnalysisHouseMapper::toDto);
    return ResponseEntity.ok(dtoPage);
  }

  @GetMapping("/{id}")
  public ResponseEntity<AnalysisHouseResponseDto> getById(@PathVariable String id) {
    try {
      UUID analysisHouseId = UUID.fromString(id);
      return analysisHouseService
          .findById(analysisHouseId)
          .map(analysisHouse -> ResponseEntity.ok(AnalysisHouseMapper.toDto(analysisHouse)))
          .orElse(ResponseEntity.notFound().build());
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }

  @PutMapping("/{id}")
  public ResponseEntity<AnalysisHouseResponseDto> update(
      @PathVariable String id, @RequestBody AnalysisHouseCreateRequestDto dto) {
    try {
      // Convert string ID to UUID, handling potential format issues
      UUID analysisHouseId;
      try {
        analysisHouseId = UUID.fromString(id);
      } catch (IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
      }

      return analysisHouseService
          .findById(analysisHouseId)
          .map(
              existing -> {
                // Get logged-in user
                User user = SecurityUtil.getLoggedUser();
                // Map DTO to entity and set ID
                AnalysisHouse analysisHouseToUpdate = AnalysisHouseMapper.toEntity(dto, user);
                analysisHouseToUpdate.setId(analysisHouseId);
                AnalysisHouse updated = analysisHouseService.save(analysisHouseToUpdate);
                return ResponseEntity.ok(AnalysisHouseMapper.toDto(updated));
              })
          .orElse(ResponseEntity.notFound().build());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PostMapping
  public ResponseEntity<AnalysisHouseResponseDto> create(
      @RequestBody AnalysisHouseCreateRequestDto dto) {
    User user = SecurityUtil.getLoggedUser();
    AnalysisHouse analysisHouse = AnalysisHouseMapper.toEntity(dto, user);

    return analysisHouseService
        .findByName(analysisHouse.getName())
        .map(
            existing ->
                ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(AnalysisHouseMapper.toDto(existing)))
        .orElseGet(
            () -> {
              AnalysisHouse savedAnalysisHouse = analysisHouseService.save(analysisHouse);
              return ResponseEntity.status(HttpStatus.CREATED)
                  .body(AnalysisHouseMapper.toDto(savedAnalysisHouse));
            });
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable String id) {
    try {
      UUID analysisHouseId = UUID.fromString(id);
      if (analysisHouseService.findById(analysisHouseId).isPresent()) {
        analysisHouseService.deleteById(analysisHouseId);
        return ResponseEntity.noContent().build();
      } else {
        return ResponseEntity.notFound().build();
      }
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }
}
