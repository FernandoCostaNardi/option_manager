package com.olisystem.optionsmanager.controller;

import com.olisystem.optionsmanager.dto.OperationDataRequest;
import com.olisystem.optionsmanager.dto.OperationFilterCriteria;
import com.olisystem.optionsmanager.dto.OperationSummaryResponseDto;
import com.olisystem.optionsmanager.model.OperationStatus;
import com.olisystem.optionsmanager.model.OptionType;
import com.olisystem.optionsmanager.model.TradeType;
import com.olisystem.optionsmanager.model.TransactionType;
import com.olisystem.optionsmanager.service.OperationService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class OperationController {

  @Autowired private OperationService operationService;

  @PostMapping("/operations")
  public ResponseEntity<?> createOperation(@RequestBody OperationDataRequest request) {
    try {
      operationService.createOperation(request);
      return ResponseEntity.ok().body("Operação criada com sucesso!");
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body("Erro ao criar operação: " + e.getMessage());
    }
  }

  @GetMapping("/operations")
  public ResponseEntity<Page<OperationSummaryResponseDto>> getOperationsByStatus(
      @RequestParam(required = false) List<OperationStatus> status,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate entryDateStart,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate entryDateEnd,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate exitDateStart,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate exitDateEnd,
      @RequestParam(required = false) String analysisHouseName,
      @RequestParam(required = false) String brokerageName,
      @RequestParam(required = false) TransactionType transactionType,
      @RequestParam(required = false) TradeType tradeType,
      @RequestParam(required = false) OptionType optionType,
      @PageableDefault(size = 5) Pageable pageable) {

    OperationFilterCriteria filterCriteria =
        OperationFilterCriteria.builder()
            .status(status)
            .entryDateStart(entryDateStart)
            .entryDateEnd(entryDateEnd)
            .exitDateStart(exitDateStart)
            .exitDateEnd(exitDateEnd)
            .analysisHouseName(analysisHouseName)
            .brokerageName(brokerageName)
            .transactionType(transactionType)
            .tradeType(tradeType)
            .optionType(optionType)
            .build();

    Page<OperationSummaryResponseDto> result =
        operationService.findByFilters(filterCriteria, pageable);
    return ResponseEntity.ok(result);
  }
}
