package com.olisystem.optionsmanager.controller.operation;

import com.olisystem.optionsmanager.dto.operation.OperationDataRequest;
import com.olisystem.optionsmanager.dto.operation.OperationFilterCriteria;
import com.olisystem.optionsmanager.dto.operation.OperationSummaryResponseDto;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationStatus;
import com.olisystem.optionsmanager.model.operation.TradeType;
import com.olisystem.optionsmanager.model.option_serie.OptionType;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import com.olisystem.optionsmanager.record.operation.OperationSearchRequest;
import com.olisystem.optionsmanager.report.OperationReportService;
import com.olisystem.optionsmanager.service.operation.OperationService;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.olisystem.optionsmanager.service.operation.search.OperationSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
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

  private final OperationService operationService;
  private final OperationReportService operationReportService;
  private final OperationSearchService operationSearchService;

  public OperationController(OperationService operationService,
                             OperationReportService operationReportService,
                             OperationSearchService operationSearchService
  ){
    this.operationService = operationService;
    this.operationReportService = operationReportService;
    this.operationSearchService = operationSearchService;
  }

  @PostMapping("/operations")
  public ResponseEntity<?> createOperation(@RequestBody OperationDataRequest request) {
    try {
      Operation newOperation = operationService.createOperation(request);
      return ResponseEntity.ok(newOperation);
    } catch (Exception e) {
      log.error("Erro ao criar operação: {}", e.getMessage());
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }

  @GetMapping("/operations")
  public ResponseEntity<OperationSummaryResponseDto> getOperations(
          OperationSearchRequest searchRequest, Pageable pageable) {

    OperationSummaryResponseDto result = operationSearchService.searchOperations(searchRequest, pageable);
    return ResponseEntity.ok(result);
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

  //  @PostMapping("/operations/finalize")
  //  public ResponseEntity<?> finalizeOperation(
  //      @RequestBody OperationFinalizationRequest request,
  //      @RequestParam(defaultValue = "0") int page,
  //      @RequestParam(defaultValue = "10") int size,
  //      @RequestParam(required = false) List<OperationStatus> status) {
  //    try {
  //      OperationFinalizationResponse response =
  //          operationService.finalizeOperation(request, page, size, status);
  //      return ResponseEntity.ok(response);
  //    } catch (Exception e) {
  //      return ResponseEntity.internalServerError()
  //          .body("Erro ao finalizar operação: " + e.getMessage());
  //    }
  //  }
  //
  //  @PostMapping("/operations/finalize-parcial")
  //  public ResponseEntity<?> finalizeParcialOperation(
  //      @RequestBody OperationFinalizationRequest request) {
  //    try {
  //      OperationFinalizationResponse response =
  // operationService.finalizeParcialOperation(request);
  //      return ResponseEntity.ok(response);
  //    } catch (Exception e) {
  //      return ResponseEntity.internalServerError()
  //          .body("Erro ao finalizar operação: " + e.getMessage());
  //    }
  //  }
  //

  //
  //  @GetMapping("/operations/export/excel")
  //  public ResponseEntity<byte[]> exportOperationsToExcel(
  //      @RequestParam(value = "status[]", required = false) List<OperationStatus> status,
  //      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  //          LocalDate entryDateStart,
  //      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  //          LocalDate entryDateEnd,
  //      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  //          LocalDate exitDateStart,
  //      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  //          LocalDate exitDateEnd,
  //      @RequestParam(required = false) String analysisHouseName,
  //      @RequestParam(required = false) String brokerageName,
  //      @RequestParam(required = false) TransactionType transactionType,
  //      @RequestParam(required = false) TradeType tradeType,
  //      @RequestParam(required = false) OptionType optionType,
  //      @RequestParam(required = false) String optionSeriesCode) {
  //
  //    log.info("Status recebido: {}", status);
  //
  //    List<OperationStatus> finalStatusList;
  //    if (status != null && !status.isEmpty()) {
  //      finalStatusList = status;
  //      log.info("Usando status fornecido: {}", finalStatusList);
  //    } else {
  //      log.info("Nenhum status fornecido, usando ACTIVE como padrão");
  //      finalStatusList = Collections.singletonList(OperationStatus.ACTIVE);
  //    }
  //
  //    OperationFilterCriteria filterCriteria =
  //        OperationFilterCriteria.builder()
  //            .status(finalStatusList)
  //            .entryDateStart(entryDateStart)
  //            .entryDateEnd(entryDateEnd)
  //            .exitDateStart(exitDateStart)
  //            .exitDateEnd(exitDateEnd)
  //            .analysisHouseName(analysisHouseName)
  //            .brokerageName(brokerageName)
  //            .transactionType(transactionType)
  //            .tradeType(tradeType)
  //            .optionType(optionType)
  //            .optionSeriesCode(optionSeriesCode)
  //            .build();
  //
  //    log.info("Critérios de filtro: {}", filterCriteria);
  //
  //    byte[] excelBytes = operationReportService.generateExcelReport(filterCriteria);
  //
  //    HttpHeaders headers = new HttpHeaders();
  //    headers.setContentType(
  //        MediaType.parseMediaType(
  //            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
  //    headers.setContentDispositionFormData("attachment", "operations.xlsx");
  //    headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
  //
  //    return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
  //  }
  //
  //  @GetMapping("/operations/export/pdf")
  //  public ResponseEntity<byte[]> exportOperationsToPdf(
  //      @RequestParam(value = "status[]", required = false) List<OperationStatus> status,
  //      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  //          LocalDate entryDateStart,
  //      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  //          LocalDate entryDateEnd,
  //      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  //          LocalDate exitDateStart,
  //      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  //          LocalDate exitDateEnd,
  //      @RequestParam(required = false) String analysisHouseName,
  //      @RequestParam(required = false) String brokerageName,
  //      @RequestParam(required = false) TransactionType transactionType,
  //      @RequestParam(required = false) TradeType tradeType,
  //      @RequestParam(required = false) OptionType optionType,
  //      @RequestParam(required = false) String optionSeriesCode) {
  //
  //    log.info("Status recebido para PDF: {}", status);
  //
  //    List<OperationStatus> finalStatusList;
  //    if (status != null && !status.isEmpty()) {
  //      finalStatusList = status;
  //      log.info("Usando status fornecido para PDF: {}", finalStatusList);
  //    } else {
  //      log.info("Nenhum status fornecido para PDF, usando ACTIVE como padrão");
  //      finalStatusList = Collections.singletonList(OperationStatus.ACTIVE);
  //    }
  //
  //    OperationFilterCriteria filterCriteria =
  //        OperationFilterCriteria.builder()
  //            .status(finalStatusList)
  //            .entryDateStart(entryDateStart)
  //            .entryDateEnd(entryDateEnd)
  //            .exitDateStart(exitDateStart)
  //            .exitDateEnd(exitDateEnd)
  //            .analysisHouseName(analysisHouseName)
  //            .brokerageName(brokerageName)
  //            .transactionType(transactionType)
  //            .tradeType(tradeType)
  //            .optionType(optionType)
  //            .optionSeriesCode(optionSeriesCode)
  //            .build();
  //
  //    log.info("Critérios de filtro para PDF: {}", filterCriteria);
  //
  //    byte[] pdfBytes = operationReportService.generatePdfReport(filterCriteria);
  //
  //    HttpHeaders headers = new HttpHeaders();
  //    headers.setContentType(MediaType.APPLICATION_PDF);
  //    headers.setContentDispositionFormData("attachment", "operations.pdf");
  //    headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
  //
  //    return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
  //  }
}
