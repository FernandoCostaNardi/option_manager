package com.olisystem.optionsmanager.controller.invoice;

import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.service.invoice.InvoiceImportService;

import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/invoice-import")
@RequiredArgsConstructor
public class InvoiceImportController {

  private final InvoiceImportService invoiceImportService;

  @PostMapping("/upload")
  public ResponseEntity<List<Invoice>> uploadInvoices(
      @RequestParam("files") List<MultipartFile> files) throws IOException {
    List<Invoice> importedInvoices = invoiceImportService.importInvoices(files);
    return ResponseEntity.ok(importedInvoices);
  }
}
