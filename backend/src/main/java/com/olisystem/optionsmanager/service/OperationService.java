package com.olisystem.optionsmanager.service;

import com.itextpdf.text.*;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.pdf.*;
import com.olisystem.optionsmanager.dto.OperationDataRequest;
import com.olisystem.optionsmanager.dto.OperationFilterCriteria;
import com.olisystem.optionsmanager.dto.OperationItemDto;
import com.olisystem.optionsmanager.dto.OperationSummaryResponseDto;
import com.olisystem.optionsmanager.model.AnalysisHouse;
import com.olisystem.optionsmanager.model.Asset;
import com.olisystem.optionsmanager.model.Brokerage;
import com.olisystem.optionsmanager.model.Operation;
import com.olisystem.optionsmanager.model.OperationStatus;
import com.olisystem.optionsmanager.model.OperationTarget;
import com.olisystem.optionsmanager.model.OptionSerie;
import com.olisystem.optionsmanager.model.TradeType;
import com.olisystem.optionsmanager.model.User;
import com.olisystem.optionsmanager.repository.OperationRepository;
import com.olisystem.optionsmanager.repository.OperationTargetRepository;
import com.olisystem.optionsmanager.repository.OptionSerieRepository;
import com.olisystem.optionsmanager.util.OperationSummaryCalculator;
import com.olisystem.optionsmanager.util.SecurityUtil;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class OperationService {

  @Autowired private OperationRepository operationRepository;
  @Autowired private AssetService assetService;
  @Autowired private OptionSerieService optionSerieService;
  @Autowired private OptionSerieRepository optionSerieRepository;
  @Autowired private AnalysisHouseService analysisHouseService;
  @Autowired private BrokerageService brokerageService;
  @Autowired private OperationTargetRepository operationTargetRepository;

  public void createOperation(OperationDataRequest request) {
    log.info("indo buscar o : " + request.getBaseAssetCode());
    // 1. Busca ou salva o Asset
    Asset asset = assetService.getAssetByCode(request.getBaseAssetCode());
    log.info("Asset buscou e encontrou: " + asset);
    if (asset == null) {
      asset = new Asset();
      asset.setCode(request.getBaseAssetCode().toLowerCase());
      asset.setName(request.getBaseAssetName());
      asset.setType(request.getBaseAssetType());
      asset.setUrlLogo(request.getBaseAssetLogoUrl());
      asset = assetService.save(asset);
    }

    // 2. Busca ou salva o OptionSerie
    OptionSerie optionSerie =
        optionSerieService.getOptionSerieByCode(request.getOptionSeriesCode());
    if (optionSerie == null) {
      optionSerie = new OptionSerie();
      optionSerie.setCode(request.getOptionSeriesCode().toUpperCase());
      optionSerie.setType(request.getOptionSeriesType());
      optionSerie.setStrikePrice(request.getOptionSeriesStrikePrice());
      optionSerie.setExpirationDate(request.getOptionSeriesExpirationDate());
      optionSerie.setAsset(asset);
      optionSerie = optionSerieRepository.save(optionSerie);
    }

    // 3. Salva Operation
    Operation operation = new Operation();
    operation.setOptionSeries(optionSerie);
    Brokerage brokerage = brokerageService.getBrokerageById(request.getBrokerageId());
    operation.setBrokerage(brokerage);

    if (request.getAnalysisHouseId() != null) {
      Optional<AnalysisHouse> analysisHouseOpt =
          analysisHouseService.findById(request.getAnalysisHouseId());
      analysisHouseOpt.ifPresent(operation::setAnalysisHouse);
    }

    operation.setTransactionType(request.getTransactionType());
    if (request.getExitDate() != null) {
      operation.setTradeType(calculateTradeType(request.getEntryDate(), request.getExitDate()));
    }
    operation.setEntryDate(request.getEntryDate());
    operation.setExitDate(request.getExitDate());
    operation.setQuantity(request.getQuantity());
    operation.setEntryUnitPrice(request.getEntryUnitPrice());
    operation.setEntryTotalValue(
        request.getEntryUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
    operation.setStatus(OperationStatus.ACTIVE);
    operation.setUser(SecurityUtil.getLoggedUser());

    // Salva a Operation e guarda a referência final
    final Operation savedOperation = operationRepository.save(operation);

    // 4. Salva OperationTargets
    List<OperationTarget> targets = request.getTargets();
    if (targets != null && !targets.isEmpty()) {
      List<OperationTarget> newTargets =
          targets.stream()
              .map(
                  targetDto -> {
                    OperationTarget target = new OperationTarget();
                    target.setSequence(targetDto.getSequence());
                    target.setType(targetDto.getType());
                    target.setValue(targetDto.getValue());
                    target.setOperation(savedOperation); // Usando a referência final
                    return target;
                  })
              .collect(Collectors.toList());

      operationTargetRepository.saveAll(newTargets);
    }
  }

  private TradeType calculateTradeType(LocalDate entryDate, LocalDate exitDate) {
    if (entryDate.isBefore(exitDate)) {
      return TradeType.SWING;
    } else {
      return TradeType.DAY;
    }
  }

  public Page<OperationSummaryResponseDto> findByStatuses(
      List<OperationStatus> statuses, Pageable pageable) {
    // Verificar se há ordenação por campos que não existem na entidade
    if (pageable.getSort().isSorted()) {
      // Extrair a ordenação original
      Sort sort = pageable.getSort();

      // Criar um novo Pageable sem ordenação
      Pageable pageableWithoutSort =
          PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());

      // Buscar as operações do usuário logado
      User user = SecurityUtil.getLoggedUser();
      Page<Operation> page =
          operationRepository.findByUserAndStatusIn(user, statuses, pageableWithoutSort);

      // Converter para DTOs
      List<OperationItemDto> dtos =
          page.getContent().stream().map(this::mapToDto).collect(Collectors.toList());

      // Ordenar manualmente a lista de DTOs
      List<OperationItemDto> sortedDtos = sortDtoList(dtos, sort);

      // Calcular totalizadores usando a classe utilitária
      OperationSummaryResponseDto summary = OperationSummaryCalculator.calculateSummary(sortedDtos);

      // Criar uma nova página com os DTOs ordenados
      return new PageImpl<>(List.of(summary), pageable, page.getTotalElements());
    } else {
      // Se não houver ordenação, usar o fluxo normal
      User user = SecurityUtil.getLoggedUser();
      Page<Operation> page = operationRepository.findByUserAndStatusIn(user, statuses, pageable);

      // Converter para DTOs
      List<OperationItemDto> dtos =
          page.getContent().stream().map(this::mapToDto).collect(Collectors.toList());

      // Calcular totalizadores usando a classe utilitária
      OperationSummaryResponseDto summary = OperationSummaryCalculator.calculateSummary(dtos);

      // Criar uma nova página
      return new PageImpl<>(List.of(summary), pageable, page.getTotalElements());
    }
  }

  // Método auxiliar para ordenar a lista de DTOs
  private List<OperationItemDto> sortDtoList(List<OperationItemDto> dtos, Sort sort) {
    List<OperationItemDto> sortedDtos = dtos;
    for (Sort.Order order : sort) {
      String property = order.getProperty();
      boolean isAscending = order.getDirection().isAscending();
      sortedDtos.sort(createComparator(property, isAscending));
    }
    return sortedDtos;
  }

  // Método para criar um comparador baseado na propriedade
  private Comparator<OperationItemDto> createComparator(String property, boolean isAscending) {
    Comparator<OperationItemDto> comparator;
    switch (property) {
      case "optionSeriesCode": // Certifique-se de que o nome está correto aqui
        comparator =
            Comparator.comparing(
                OperationItemDto::getOptionSeriesCode, String.CASE_INSENSITIVE_ORDER);
        break;
      case "analysisHouseName":
        comparator =
            Comparator.comparing(
                dto ->
                    dto.getAnalysisHouseName() != null
                        ? dto.getAnalysisHouseName().toLowerCase()
                        : "",
                Comparator.nullsLast(String::compareTo));
        break;
      case "brokerageName":
        comparator =
            Comparator.comparing(
                dto -> dto.getBrokerageName() != null ? dto.getBrokerageName().toLowerCase() : "",
                Comparator.nullsLast(String::compareTo));
        break;
      case "baseAssetLogoUrl":
        comparator =
            Comparator.comparing(
                dto -> dto.getBaseAssetLogoUrl() != null ? dto.getBaseAssetLogoUrl() : "",
                Comparator.nullsLast(String::compareTo));
        break;
      case "transactionType":
        comparator =
            Comparator.comparing(
                OperationItemDto::getTransactionType, Comparator.nullsLast(Enum::compareTo));
        break;
      case "tradeType":
        comparator =
            Comparator.comparing(
                OperationItemDto::getTradeType, Comparator.nullsLast(Enum::compareTo));
        break;
      case "status":
        comparator =
            Comparator.comparing(
                OperationItemDto::getStatus, Comparator.nullsLast(Enum::compareTo));
        break;
      case "optionType":
        comparator =
            Comparator.comparing(
                OperationItemDto::getOptionType, Comparator.nullsLast(Enum::compareTo));
        break;
      default:
        // Para outras propriedades, tentar usar reflection
        comparator = (o1, o2) -> 0; // Comparador neutro como fallback
    }
    return isAscending ? comparator : comparator.reversed();
  }

  private Specification<Operation> createSpecification(OperationFilterCriteria criteria) {
    Specification<Operation> spec = Specification.where(null);

    // Filtro por usuário logado
    User user = SecurityUtil.getLoggedUser();
    spec = spec.and((root, query, cb) -> cb.equal(root.get("user"), user));

    // Filtro por status
    if (criteria.getStatus() != null && !criteria.getStatus().isEmpty()) {
      spec = spec.and((root, query, cb) -> root.get("status").in(criteria.getStatus()));
    }

    // Filtro por código da série de opções (corrigido)
    if (criteria.getOptionSeriesCode() != null && !criteria.getOptionSeriesCode().isEmpty()) {
      spec =
          spec.and(
              (root, query, cb) ->
                  cb.like(
                      cb.lower(root.join("optionSeries").get("code")),
                      "%" + criteria.getOptionSeriesCode().toLowerCase() + "%"));
    }

    // Filtro por data de entrada
    if (criteria.getEntryDateStart() != null) {
      spec =
          spec.and(
              (root, query, cb) ->
                  cb.greaterThanOrEqualTo(root.get("entryDate"), criteria.getEntryDateStart()));
    }

    if (criteria.getEntryDateEnd() != null) {
      spec =
          spec.and(
              (root, query, cb) ->
                  cb.lessThanOrEqualTo(root.get("entryDate"), criteria.getEntryDateEnd()));
    }

    // Filtro por data de saída
    if (criteria.getExitDateStart() != null) {
      spec =
          spec.and(
              (root, query, cb) ->
                  cb.greaterThanOrEqualTo(root.get("exitDate"), criteria.getExitDateStart()));
    }

    if (criteria.getExitDateEnd() != null) {
      spec =
          spec.and(
              (root, query, cb) ->
                  cb.lessThanOrEqualTo(root.get("exitDate"), criteria.getExitDateEnd()));
    }

    // Filtro por tipo de transação
    if (criteria.getTransactionType() != null) {
      spec =
          spec.and(
              (root, query, cb) ->
                  cb.equal(root.get("transactionType"), criteria.getTransactionType()));
    }

    // Filtro por tipo de trade
    if (criteria.getTradeType() != null) {
      spec =
          spec.and((root, query, cb) -> cb.equal(root.get("tradeType"), criteria.getTradeType()));
    }

    // Filtro por tipo de opção
    if (criteria.getOptionType() != null) {
      spec =
          spec.and(
              (root, query, cb) ->
                  cb.equal(root.join("optionSeries").get("type"), criteria.getOptionType()));
    }

    // Filtros adicionais para análise e corretora se forem adicionados
    if (criteria.getAnalysisHouseName() != null && !criteria.getAnalysisHouseName().isEmpty()) {
      spec =
          spec.and(
              (root, query, cb) ->
                  cb.like(
                      cb.lower(root.join("analysisHouse").get("name")),
                      "%" + criteria.getAnalysisHouseName().toLowerCase() + "%"));
    }

    if (criteria.getBrokerageName() != null && !criteria.getBrokerageName().isEmpty()) {
      spec =
          spec.and(
              (root, query, cb) ->
                  cb.like(
                      cb.lower(root.join("brokerage").get("name")),
                      "%" + criteria.getBrokerageName().toLowerCase() + "%"));
    }

    return spec;
  }

  public Page<OperationSummaryResponseDto> findByFilters(
      OperationFilterCriteria criteria, Pageable pageable) {

    // Verificar se há ordenação por campos que não existem na entidade
    if (pageable.getSort().isSorted()) {
      // Verificar se algum dos campos de ordenação é um campo derivado
      boolean hasCustomSort = false;
      for (Sort.Order order : pageable.getSort()) {
        if (order.getProperty().equals("optionSeriesCode")
            || order.getProperty().equals("analysisHouseName")
            || order.getProperty().equals("brokerageName")
            || order.getProperty().equals("baseAssetLogoUrl")
            || order.getProperty().equals("transactionType")
            || order.getProperty().equals("tradeType")
            || order.getProperty().equals("status")
            || order.getProperty().equals("optionType")) {
          hasCustomSort = true;
          break;
        }
      }

      if (hasCustomSort) {
        // Extrair a ordenação original
        Sort sort = pageable.getSort();

        // Criar um novo Pageable sem ordenação
        Pageable pageableWithoutSort =
            PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());

        // Aplicar filtros
        Specification<Operation> spec = createSpecification(criteria);

        // Buscar as operações sem ordenação
        Page<Operation> page = operationRepository.findAll(spec, pageableWithoutSort);

        // Converter para DTOs
        List<OperationItemDto> dtos =
            page.getContent().stream().map(this::mapToDto).collect(Collectors.toList());

        // Ordenar manualmente a lista de DTOs
        List<OperationItemDto> sortedDtos = sortDtoList(dtos, sort);

        // Calcular totalizadores usando a classe utilitária
        OperationSummaryResponseDto summary =
            OperationSummaryCalculator.calculateSummary(sortedDtos);

        // Criar uma nova página com os DTOs ordenados
        return new PageImpl<>(List.of(summary), pageable, page.getTotalElements());
      }
    }

    // Se não houver ordenação personalizada, usar o fluxo normal
    Specification<Operation> spec = createSpecification(criteria);
    Page<Operation> operations = operationRepository.findAll(spec, pageable);

    // Mapear para DTOs
    List<OperationItemDto> dtos =
        operations.getContent().stream().map(this::mapToDto).collect(Collectors.toList());

    // Calcular totalizadores
    OperationSummaryResponseDto summary = OperationSummaryCalculator.calculateSummary(dtos);

    // Retornar a página de resumo
    return new PageImpl<>(List.of(summary), pageable, operations.getTotalElements());
  }

  private OperationItemDto mapToDto(Operation op) {
    return OperationItemDto.builder()
        .id(op.getId())
        .optionSeriesCode(
            op.getOptionSeries()
                .getCode()
                .toUpperCase()) // Convertendo para maiúsculas para manter consistência
        .optionType(op.getOptionSeries().getType())
        .transactionType(op.getTransactionType())
        .tradeType(op.getTradeType())
        .entryDate(op.getEntryDate())
        .exitDate(op.getExitDate())
        .status(op.getStatus())
        .analysisHouseName(op.getAnalysisHouse() != null ? op.getAnalysisHouse().getName() : null)
        .brokerageName(op.getBrokerage() != null ? op.getBrokerage().getName() : null)
        .quantity(op.getQuantity())
        .entryUnitPrice(op.getEntryUnitPrice())
        .entryTotalValue(op.getEntryTotalValue())
        .exitUnitPrice(op.getExitUnitPrice())
        .exitTotalValue(op.getExitTotalValue())
        .profitLoss(op.getProfitLoss())
        .profitLossPercentage(op.getProfitLossPercentage())
        .baseAssetLogoUrl(op.getOptionSeries().getAsset().getUrlLogo())
        .build();
  }

  public byte[] generateExcelReport(OperationFilterCriteria filterCriteria) {
    // Buscar todas as operações que correspondem aos critérios de filtro
    // Não usamos paginação aqui para exportar todos os dados
    Pageable unpaged = Pageable.unpaged();
    Page<OperationSummaryResponseDto> result = findByFilters(filterCriteria, unpaged);
    List<OperationItemDto> operations = result.getContent().get(0).getOperations();

    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {

      Sheet sheet = workbook.createSheet("Operações");

      // Estilo para o cabeçalho
      org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
      headerFont.setBold(true);
      CellStyle headerCellStyle = workbook.createCellStyle();
      headerCellStyle.setFont(headerFont);

      // Criar cabeçalho
      Row headerRow = sheet.createRow(0);
      String[] columns = {
        "Código",
        "Casa de Análise",
        "Corretora",
        "Tipo de Transação",
        "Data de Entrada",
        "Data de Saída",
        "Status",
        "Quantidade",
        "Preço Unitário Entrada",
        "Valor Total Entrada",
        "Preço Unitário Saída",
        "Valor Total Saída",
        "Lucro/Prejuízo",
        "Lucro/Prejuízo %"
      };

      for (int i = 0; i < columns.length; i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(columns[i]);
        cell.setCellStyle(headerCellStyle);
      }

      // Preencher dados
      int rowNum = 1;
      for (OperationItemDto operation : operations) {
        Row row = sheet.createRow(rowNum++);

        row.createCell(0).setCellValue(operation.getOptionSeriesCode());
        row.createCell(1)
            .setCellValue(
                operation.getAnalysisHouseName() != null ? operation.getAnalysisHouseName() : "");
        row.createCell(2).setCellValue(operation.getBrokerageName());
        row.createCell(3)
            .setCellValue(
                operation.getTransactionType() != null
                    ? operation.getTransactionType().toString()
                    : "");
        row.createCell(4)
            .setCellValue(
                operation.getEntryDate() != null ? operation.getEntryDate().toString() : "");
        row.createCell(5)
            .setCellValue(
                operation.getExitDate() != null ? operation.getExitDate().toString() : "");
        row.createCell(6)
            .setCellValue(operation.getStatus() != null ? operation.getStatus().toString() : "");
        row.createCell(7)
            .setCellValue(operation.getQuantity() != null ? operation.getQuantity() : 0);
        row.createCell(8)
            .setCellValue(
                operation.getEntryUnitPrice() != null
                    ? operation.getEntryUnitPrice().doubleValue()
                    : 0);
        row.createCell(9)
            .setCellValue(
                operation.getEntryTotalValue() != null
                    ? operation.getEntryTotalValue().doubleValue()
                    : 0);
        row.createCell(10)
            .setCellValue(
                operation.getExitUnitPrice() != null
                    ? operation.getExitUnitPrice().doubleValue()
                    : 0);
        row.createCell(11)
            .setCellValue(
                operation.getExitTotalValue() != null
                    ? operation.getExitTotalValue().doubleValue()
                    : 0);
        row.createCell(12)
            .setCellValue(
                operation.getProfitLoss() != null ? operation.getProfitLoss().doubleValue() : 0);
        row.createCell(13)
            .setCellValue(
                operation.getProfitLossPercentage() != null
                    ? operation.getProfitLossPercentage().doubleValue()
                    : 0);
      }

      // Adicionar linha de resumo
      Row summaryRow = sheet.createRow(rowNum + 1);
      CellStyle boldStyle = workbook.createCellStyle();
      org.apache.poi.ss.usermodel.Font boldFont = workbook.createFont();
      boldFont.setBold(true);
      boldStyle.setFont(boldFont);

      Cell summaryLabelCell = summaryRow.createCell(0);
      summaryLabelCell.setCellValue("RESUMO");
      summaryLabelCell.setCellStyle(boldStyle);

      Row totalEntryRow = sheet.createRow(rowNum + 2);
      totalEntryRow.createCell(0).setCellValue("Valor Total de Entrada:");
      Cell totalEntryCell = totalEntryRow.createCell(1);
      totalEntryCell.setCellValue(result.getContent().get(0).getTotalEntryValue().doubleValue());

      Row totalProfitRow = sheet.createRow(rowNum + 3);
      totalProfitRow.createCell(0).setCellValue("Lucro/Prejuízo Total:");
      Cell totalProfitCell = totalProfitRow.createCell(1);
      totalProfitCell.setCellValue(result.getContent().get(0).getTotalProfitLoss().doubleValue());

      Row totalPercentRow = sheet.createRow(rowNum + 4);
      totalPercentRow.createCell(0).setCellValue("Lucro/Prejuízo Total %:");
      Cell totalPercentCell = totalPercentRow.createCell(1);
      totalPercentCell.setCellValue(
          result.getContent().get(0).getTotalProfitLossPercentage().doubleValue());

      // Ajustar largura das colunas
      for (int i = 0; i < columns.length; i++) {
        sheet.autoSizeColumn(i);
      }

      workbook.write(out);
      return out.toByteArray();
    } catch (Exception e) {
      log.error("Erro ao gerar relatório Excel", e);
      throw new RuntimeException("Erro ao gerar relatório Excel: " + e.getMessage());
    }
  }

  public byte[] generatePdfReport(OperationFilterCriteria filterCriteria) {
    // Buscar todas as operações que correspondem aos critérios de filtro
    // Não usamos paginação aqui para exportar todos os dados
    Pageable unpaged = Pageable.unpaged();
    Page<OperationSummaryResponseDto> result = findByFilters(filterCriteria, unpaged);
    List<OperationItemDto> operations = result.getContent().get(0).getOperations();

    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Document document = new Document(PageSize.A4.rotate());
      PdfWriter.getInstance(document, out);
      document.open();

      // Título
      com.itextpdf.text.Font titleFont =
          new com.itextpdf.text.Font(FontFamily.HELVETICA, 18, com.itextpdf.text.Font.BOLD);
      Paragraph title = new Paragraph("Relatório de Operações", titleFont);
      title.setAlignment(Element.ALIGN_CENTER);
      document.add(title);
      document.add(Chunk.NEWLINE);

      // Tabela
      PdfPTable table = new PdfPTable(14);
      table.setWidthPercentage(100);

      // Cabeçalho
      com.itextpdf.text.Font headerFont =
          new com.itextpdf.text.Font(FontFamily.HELVETICA, 10, com.itextpdf.text.Font.BOLD);
      String[] columns = {
        "Código",
        "Casa de Análise",
        "Corretora",
        "Tipo de Transação",
        "Data de Entrada",
        "Data de Saída",
        "Status",
        "Quantidade",
        "Preço Unit. Entrada",
        "Valor Total Entrada",
        "Preço Unit. Saída",
        "Valor Total Saída",
        "Lucro/Prejuízo",
        "Lucro/Prejuízo %"
      };

      for (String column : columns) {
        PdfPCell cell = new PdfPCell(new Phrase(column, headerFont));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        table.addCell(cell);
      }

      // Dados
      com.itextpdf.text.Font dataFont = new com.itextpdf.text.Font(FontFamily.HELVETICA, 8);
      for (OperationItemDto operation : operations) {
        table.addCell(new Phrase(operation.getOptionSeriesCode(), dataFont));
        table.addCell(
            new Phrase(
                operation.getAnalysisHouseName() != null ? operation.getAnalysisHouseName() : "",
                dataFont));
        table.addCell(new Phrase(operation.getBrokerageName(), dataFont));
        table.addCell(
            new Phrase(
                operation.getTransactionType() != null
                    ? operation.getTransactionType().toString()
                    : "",
                dataFont));
        table.addCell(
            new Phrase(
                operation.getEntryDate() != null ? operation.getEntryDate().toString() : "",
                dataFont));
        table.addCell(
            new Phrase(
                operation.getExitDate() != null ? operation.getExitDate().toString() : "",
                dataFont));
        table.addCell(
            new Phrase(
                operation.getStatus() != null ? operation.getStatus().toString() : "", dataFont));
        table.addCell(
            new Phrase(
                operation.getQuantity() != null ? operation.getQuantity().toString() : "0",
                dataFont));
        table.addCell(
            new Phrase(
                operation.getEntryUnitPrice() != null
                    ? operation.getEntryUnitPrice().toString()
                    : "0",
                dataFont));
        table.addCell(
            new Phrase(
                operation.getEntryTotalValue() != null
                    ? operation.getEntryTotalValue().toString()
                    : "0",
                dataFont));
        table.addCell(
            new Phrase(
                operation.getExitUnitPrice() != null
                    ? operation.getExitUnitPrice().toString()
                    : "0",
                dataFont));
        table.addCell(
            new Phrase(
                operation.getExitTotalValue() != null
                    ? operation.getExitTotalValue().toString()
                    : "0",
                dataFont));
        table.addCell(
            new Phrase(
                operation.getProfitLoss() != null ? operation.getProfitLoss().toString() : "0",
                dataFont));
        table.addCell(
            new Phrase(
                operation.getProfitLossPercentage() != null
                    ? operation.getProfitLossPercentage().toString()
                    : "0",
                dataFont));
      }

      document.add(table);
      document.add(Chunk.NEWLINE);

      // Resumo
      com.itextpdf.text.Font summaryFont =
          new com.itextpdf.text.Font(FontFamily.HELVETICA, 12, com.itextpdf.text.Font.BOLD);
      Paragraph summary = new Paragraph("RESUMO", summaryFont);
      document.add(summary);

      Paragraph totalEntry =
          new Paragraph(
              "Valor Total de Entrada: "
                  + result.getContent().get(0).getTotalEntryValue().toString());
      document.add(totalEntry);

      Paragraph totalProfit =
          new Paragraph(
              "Lucro/Prejuízo Total: "
                  + result.getContent().get(0).getTotalProfitLoss().toString());
      document.add(totalProfit);

      Paragraph totalPercent =
          new Paragraph(
              "Lucro/Prejuízo Total %: "
                  + result.getContent().get(0).getTotalProfitLossPercentage().toString());
      document.add(totalPercent);

      document.close();
      return out.toByteArray();
    } catch (Exception e) {
      log.error("Erro ao gerar relatório PDF", e);
      throw new RuntimeException("Erro ao gerar relatório PDF: " + e.getMessage());
    }
  }
}
