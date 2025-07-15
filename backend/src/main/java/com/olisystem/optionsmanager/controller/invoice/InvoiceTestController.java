package com.olisystem.optionsmanager.controller.invoice;

import com.olisystem.optionsmanager.dto.invoice.InvoiceProcessingRequest;
import com.olisystem.optionsmanager.dto.invoice.InvoiceProcessingResponse;
import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/invoice/test")
@RequiredArgsConstructor
@Slf4j
public class InvoiceTestController {

    private final InvoiceRepository invoiceRepository;

    @PostMapping("/process")
    public ResponseEntity<InvoiceProcessingResponse> processInvoice(@RequestBody InvoiceProcessingRequest request) {
        log.info("Iniciando processamento da invoice: {}", request.getInvoiceId());
        
        try {
            UUID invoiceId = UUID.fromString(request.getInvoiceId());
            Invoice invoice = invoiceRepository.findById(invoiceId)
                    .orElseThrow(() -> new RuntimeException("Invoice não encontrada: " + request.getInvoiceId()));
            
            log.info("Invoice encontrada: {} - {}", invoice.getInvoiceNumber(), invoice.getTradingDate());
            
            // Simular processamento
            InvoiceProcessingResponse response = InvoiceProcessingResponse.builder()
                    .success(true)
                    .message("Invoice processada com sucesso")
                    .invoiceId(request.getInvoiceId())
                    .processingTime(1500L)
                    .operationsCreated(5)
                    .operationsUpdated(2)
                    .operationsSkipped(1)
                    .build();
            
            log.info("Processamento concluído: {}", response);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erro ao processar invoice: {}", e.getMessage(), e);
            
            InvoiceProcessingResponse response = InvoiceProcessingResponse.builder()
                    .success(false)
                    .message("Erro ao processar invoice: " + e.getMessage())
                    .invoiceId(request.getInvoiceId())
                    .processingTime(0L)
                    .operationsCreated(0)
                    .operationsUpdated(0)
                    .operationsSkipped(0)
                    .build();
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Invoice Test Controller está funcionando!");
    }
} 