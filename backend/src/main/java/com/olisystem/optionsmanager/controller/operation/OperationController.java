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
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class OperationController {

  private static final Logger log = LoggerFactory.getLogger(OperationController.class);

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

  @PostMapping("/operations/{id}")
  public ResponseEntity<?> updateOperation(
      @PathVariable UUID id, @RequestBody OperationDataRequest request) {
    try {
      operationService.updateOperation(id, request);
      return ResponseEntity.ok().body("Operação atualizada com sucesso!");
    } catch (Exception e) {
      return ResponseEntity.internalServerError()
          .body("Erro ao atualizar operação: " + e.getMessage());
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
      @RequestParam(required = false) String optionSeriesCode,
      Pageable pageable) {

    // Se status não for fornecido, use ACTIVE como padrão
    if (status == null || status.isEmpty()) {
      status = Collections.singletonList(OperationStatus.ACTIVE);
    }

    // Corrigir o problema de ordenação com optionSerieCode vs optionSeriesCode
    if (pageable.getSort().isSorted()) {
      for (Sort.Order order : pageable.getSort()) {
        if (order.getProperty().equals("optionSerieCode")) {
          // Criar um novo Sort com o nome correto da propriedade
          Sort newSort = Sort.by(order.getDirection(), "optionSeriesCode");

          // Criar um novo Pageable com o Sort corrigido
          pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), newSort);
          break;
        }
      }
    }

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
            .optionSeriesCode(optionSeriesCode)
            .build();

    Page<OperationSummaryResponseDto> result =
        operationService.findByFilters(filterCriteria, pageable);
    return ResponseEntity.ok(result);
  }

  @GetMapping("/operations/export/excel")
  public ResponseEntity<byte[]> exportOperationsToExcel(
      @RequestParam(value = "status[]", required = false) List<OperationStatus> status,
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
      @RequestParam(required = false) String optionSeriesCode) {

    log.info("Status recebido: {}", status);

    List<OperationStatus> finalStatusList;
    if (status != null && !status.isEmpty()) {
      finalStatusList = status;
      log.info("Usando status fornecido: {}", finalStatusList);
    } else {
      log.info("Nenhum status fornecido, usando ACTIVE como padrão");
      finalStatusList = Collections.singletonList(OperationStatus.ACTIVE);
    }

    OperationFilterCriteria filterCriteria =
        OperationFilterCriteria.builder()
            .status(finalStatusList)
            .entryDateStart(entryDateStart)
            .entryDateEnd(entryDateEnd)
            .exitDateStart(exitDateStart)
            .exitDateEnd(exitDateEnd)
            .analysisHouseName(analysisHouseName)
            .brokerageName(brokerageName)
            .transactionType(transactionType)
            .tradeType(tradeType)
            .optionType(optionType)
            .optionSeriesCode(optionSeriesCode)
            .build();

    log.info("Critérios de filtro: {}", filterCriteria);

    byte[] excelBytes = operationService.generateExcelReport(filterCriteria);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(
        MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    headers.setContentDispositionFormData("attachment", "operations.xlsx");
    headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

    return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
  }

  @GetMapping("/operations/export/pdf")
  public ResponseEntity<byte[]> exportOperationsToPdf(
      @RequestParam(value = "status[]", required = false) List<OperationStatus> status,
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
      @RequestParam(required = false) String optionSeriesCode) {

    log.info("Status recebido para PDF: {}", status);

    List<OperationStatus> finalStatusList;
    if (status != null && !status.isEmpty()) {
      finalStatusList = status;
      log.info("Usando status fornecido para PDF: {}", finalStatusList);
    } else {
      log.info("Nenhum status fornecido para PDF, usando ACTIVE como padrão");
      finalStatusList = Collections.singletonList(OperationStatus.ACTIVE);
    }

    OperationFilterCriteria filterCriteria =
        OperationFilterCriteria.builder()
            .status(finalStatusList)
            .entryDateStart(entryDateStart)
            .entryDateEnd(entryDateEnd)
            .exitDateStart(exitDateStart)
            .exitDateEnd(exitDateEnd)
            .analysisHouseName(analysisHouseName)
            .brokerageName(brokerageName)
            .transactionType(transactionType)
            .tradeType(tradeType)
            .optionType(optionType)
            .optionSeriesCode(optionSeriesCode)
            .build();

    log.info("Critérios de filtro para PDF: {}", filterCriteria);

    byte[] pdfBytes = operationService.generatePdfReport(filterCriteria);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDispositionFormData("attachment", "operations.pdf");
    headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

    return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
  }
}
