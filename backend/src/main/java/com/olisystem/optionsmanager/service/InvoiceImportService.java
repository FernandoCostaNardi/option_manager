package com.olisystem.optionsmanager.service;

import com.olisystem.optionsmanager.model.Invoice;
import com.olisystem.optionsmanager.repository.InvoiceRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class InvoiceImportService {

  private final InvoiceRepository invoiceRepository;
  private final PdfInvoiceExtractor pdfInvoiceExtractor;

  public List<Invoice> importInvoices(List<MultipartFile> files) throws IOException {
    List<Invoice> importedInvoices = new ArrayList<>();
    for (MultipartFile file : files) {
      Invoice invoice = pdfInvoiceExtractor.extractInvoice(file);
      invoiceRepository.save(invoice);
      importedInvoices.add(invoice);
    }
    return importedInvoices;
  }
}
