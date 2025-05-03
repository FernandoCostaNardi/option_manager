package com.olisystem.optionsmanager.service.report;

import com.itextpdf.text.*;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.pdf.*;
import com.olisystem.optionsmanager.dto.OperationFilterCriteria;
import com.olisystem.optionsmanager.dto.OperationItemDto;
import com.olisystem.optionsmanager.dto.OperationSummaryResponseDto;
import com.olisystem.optionsmanager.model.Operation;
import com.olisystem.optionsmanager.service.OperationService;
import java.io.ByteArrayOutputStream;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class OperationReportService {

  @Autowired private OperationService operationService;

  public byte[] generateExcelReport(OperationFilterCriteria filterCriteria) {
    OperationSummaryResponseDto summary = operationService.findByFilters(filterCriteria, Pageable.unpaged())
        .getContent()
        .get(0);
    List<OperationItemDto> operations = summary.getOperations();
    
    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Operações");
      
      // Cabeçalho
      Row headerRow = sheet.createRow(0);
      String[] headers = {
        "Código", "Ativo Base", "Casa de Análise", "Corretora", "Tipo", "Data Entrada",
        "Data Saída", "Quantidade", "Preço Entrada", "Preço Saída", "Valor Total",
        "Resultado", "Status"
      };
      
      for (int i = 0; i < headers.length; i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(headers[i]);
      }
      
      // Dados
      int rowNum = 1;
      for (OperationItemDto operation : operations) {
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(operation.getOptionSeriesCode());
        row.createCell(1).setCellValue(operation.getBaseAssetName());
        row.createCell(2).setCellValue(operation.getAnalysisHouseName());
        row.createCell(3).setCellValue(operation.getBrokerageName());
        row.createCell(4).setCellValue(operation.getTransactionType().name());
        row.createCell(5).setCellValue(operation.getEntryDate().toString());
        row.createCell(6).setCellValue(operation.getExitDate() != null ? operation.getExitDate().toString() : "");
        row.createCell(7).setCellValue(operation.getQuantity());
        row.createCell(8).setCellValue(operation.getEntryUnitPrice().doubleValue());
        row.createCell(9).setCellValue(operation.getExitUnitPrice() != null ? operation.getExitUnitPrice().doubleValue() : 0);
        row.createCell(10).setCellValue(operation.getEntryTotalValue().doubleValue());
        row.createCell(11).setCellValue(operation.getResult() != null ? operation.getResult().doubleValue() : 0);
        row.createCell(12).setCellValue(operation.getStatus().name());
      }
      
      // Auto-size columns
      for (int i = 0; i < headers.length; i++) {
        sheet.autoSizeColumn(i);
      }
      
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      workbook.write(outputStream);
      return outputStream.toByteArray();
    } catch (Exception e) {
      log.error("Erro ao gerar relatório Excel", e);
      throw new RuntimeException("Erro ao gerar relatório Excel", e);
    }
  }

  public byte[] generatePdfReport(OperationFilterCriteria filterCriteria) {
    OperationSummaryResponseDto summary = operationService.findByFilters(filterCriteria, Pageable.unpaged())
        .getContent()
        .get(0);
    List<OperationItemDto> operations = summary.getOperations();
    
    try {
      Document document = new Document(PageSize.A4.rotate());
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PdfWriter.getInstance(document, outputStream);
      
      document.open();
      
      // Título
      com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(FontFamily.HELVETICA, 16, com.itextpdf.text.Font.BOLD);
      Paragraph title = new Paragraph("Relatório de Operações", titleFont);
      title.setAlignment(Element.ALIGN_CENTER);
      document.add(title);
      document.add(Chunk.NEWLINE);
      
      // Tabela
      PdfPTable table = new PdfPTable(13);
      table.setWidthPercentage(100);
      
      // Cabeçalho
      String[] headers = {
        "Código", "Ativo Base", "Casa de Análise", "Corretora", "Tipo", "Data Entrada",
        "Data Saída", "Quantidade", "Preço Entrada", "Preço Saída", "Valor Total",
        "Resultado", "Status"
      };
      
      for (String header : headers) {
        PdfPCell cell = new PdfPCell(new Phrase(header));
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
      }
      
      // Dados
      for (OperationItemDto operation : operations) {
        table.addCell(operation.getOptionSeriesCode());
        table.addCell(operation.getBaseAssetName());
        table.addCell(operation.getAnalysisHouseName());
        table.addCell(operation.getBrokerageName());
        table.addCell(operation.getTransactionType().name());
        table.addCell(operation.getEntryDate().toString());
        table.addCell(operation.getExitDate() != null ? operation.getExitDate().toString() : "");
        table.addCell(String.valueOf(operation.getQuantity()));
        table.addCell(operation.getEntryUnitPrice().toString());
        table.addCell(operation.getExitUnitPrice() != null ? operation.getExitUnitPrice().toString() : "");
        table.addCell(operation.getEntryTotalValue().toString());
        table.addCell(operation.getResult() != null ? operation.getResult().toString() : "");
        table.addCell(operation.getStatus().name());
      }
      
      document.add(table);
      document.close();
      
      return outputStream.toByteArray();
    } catch (Exception e) {
      log.error("Erro ao gerar relatório PDF", e);
      throw new RuntimeException("Erro ao gerar relatório PDF", e);
    }
  }
} 