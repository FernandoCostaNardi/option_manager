package com.olisystem.optionsmanager.service;

import com.olisystem.optionsmanager.model.Brokerage;
import com.olisystem.optionsmanager.model.Invoice;
import com.olisystem.optionsmanager.model.InvoiceItem;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
public class PdfInvoiceExtractor {

  public Invoice extractInvoice(MultipartFile file) throws IOException {
    String text = extractTextFromPdf(file);
    Invoice invoice = new Invoice();

    // Exemplo de extração simples (ajuste conforme o layout real do PDF)
    Brokerage brokerage = new Brokerage();
    brokerage.setName(extractField(text, "BTG Pactual"));
    // Preencha outros campos se necessário
    invoice.setBrokerage(brokerage);
    invoice.setClientName(extractField(text, "FERNANDO COSTA NADER"));
    invoice.setCpfCnpj(extractCpfCnpj(text));
    invoice.setInvoiceNumber(extractField(text, "Nº nota"));
    invoice.setTradeDate(extractDate(text));
    invoice.setItems(extractItems(text, invoice));
    // Outras extrações...

    // Exemplo de cálculo de valores totais
    invoice.setTotalValue(
        invoice.getItems().stream().mapToDouble(InvoiceItem::getOperationValue).sum());
    // Taxas podem ser extraídas de acordo com o layout do PDF

    return invoice;
  }

  private String extractTextFromPdf(MultipartFile file) throws IOException {
    try (PDDocument document = PDDocument.load(file.getInputStream())) {
      return new PDFTextStripper().getText(document);
    }
  }

  // Métodos de extração (ajuste conforme o layout do PDF)
  private String extractField(String text, String fieldName) {
    // Exemplo: buscar o campo pelo nome
    Pattern pattern = Pattern.compile(fieldName + "[:\\s]+(.+)");
    Matcher matcher = pattern.matcher(text);
    if (matcher.find()) {
      return matcher.group(1).trim();
    }
    return "";
  }

  private String extractCpfCnpj(String text) {
    Pattern pattern =
        Pattern.compile("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}|\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}");
    Matcher matcher = pattern.matcher(text);
    if (matcher.find()) {
      return matcher.group();
    }
    return "";
  }

  private Date extractDate(String text) {
    Pattern pattern = Pattern.compile("\\d{2}/\\d{2}/\\d{4}");
    Matcher matcher = pattern.matcher(text);
    if (matcher.find()) {
      try {
        return new SimpleDateFormat("dd/MM/yyyy").parse(matcher.group());
      } catch (Exception ignored) {
      }
    }
    return null;
  }

  private List<InvoiceItem> extractItems(String text, Invoice invoice) {
    List<InvoiceItem> items = new ArrayList<>();
    // Exemplo: buscar linhas da tabela de negociações
    Pattern pattern =
        Pattern.compile(
            "(\\d+-BOVESPA)\\s+([CV])\\s+([A-Z ]+)\\s+([\\w\\d]+)\\s+([\\w\\d]+)\\s+([\\w\\d]+)\\s+(\\d+)\\s+([\\d,.]+)\\s+([\\d,.]+)\\s+([\\d,.]+)\\s+([CV])");
    Matcher matcher = pattern.matcher(text);
    while (matcher.find()) {
      InvoiceItem item = new InvoiceItem();
      item.setInvoice(invoice);
      item.setAsset(matcher.group(5));
      item.setMarketType(matcher.group(3));
      item.setOperationType(matcher.group(2).equals("C") ? "COMPRA" : "VENDA");
      item.setQuantity(Integer.parseInt(matcher.group(7)));
      item.setPrice(Double.parseDouble(matcher.group(8).replace(",", ".")));
      item.setOperationValue(Double.parseDouble(matcher.group(9).replace(",", ".")));
      items.add(item);
    }
    return items;
  }
}
