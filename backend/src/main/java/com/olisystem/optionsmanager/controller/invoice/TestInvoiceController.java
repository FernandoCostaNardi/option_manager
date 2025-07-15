package com.olisystem.optionsmanager.controller.invoice;

import com.olisystem.optionsmanager.dto.invoice.InvoiceProcessingResponse;
import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controller de teste simples para processamento de invoices
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@RestController
@RequestMapping("/api/v1/test/invoices")
@RequiredArgsConstructor
@Slf4j
public class TestInvoiceController {

    private final InvoiceRepository invoiceRepository;

    /**
     * Processa uma invoice de teste
     */
    @PostMapping("/process/{invoiceId}")
    public ResponseEntity<InvoiceProcessingResponse> processTestInvoice(@PathVariable UUID invoiceId) {
        
        log.info("🧪 TESTE: Processando invoice {} (modo teste)", invoiceId);
        
        try {
            // Simular processamento
            Thread.sleep(2000); // Simular tempo de processamento
            
            InvoiceProcessingResponse response = InvoiceProcessingResponse.builder()
                .success(true)
                .message("Processamento de teste concluído com sucesso")
                .invoiceId(invoiceId.toString())
                .processingTime(2000L)
                .operationsCreated(1)
                .operationsUpdated(0)
                .operationsSkipped(0)
                .build();
            
            log.info("✅ TESTE: Processamento concluído - Invoice: {}", invoiceId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ TESTE: Erro no processamento: {}", e.getMessage(), e);
            
            InvoiceProcessingResponse response = InvoiceProcessingResponse.builder()
                .success(false)
                .message("Erro no processamento de teste: " + e.getMessage())
                .invoiceId(invoiceId.toString())
                .processingTime(0L)
                .operationsCreated(0)
                .operationsUpdated(0)
                .operationsSkipped(0)
                .build();
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Consulta o status de uma invoice no banco de dados
     */
    @GetMapping("/status/{invoiceId}")
    public ResponseEntity<Map<String, Object>> getInvoiceStatus(@PathVariable UUID invoiceId) {
        
        log.info("🔍 TESTE: Consultando status da invoice: {}", invoiceId);
        
        try {
            Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);
            
            Map<String, Object> response = new HashMap<>();
            response.put("invoiceId", invoiceId.toString());
            response.put("found", invoice != null);
            
            if (invoice != null) {
                response.put("tradeDate", invoice.getTradeDate());
                response.put("totalValue", invoice.getTotalValue());
                response.put("totalFees", invoice.getTotalFees());
                response.put("importedAt", invoice.getImportedAt());
                response.put("createdAt", invoice.getCreatedAt());
                response.put("updatedAt", invoice.getUpdatedAt());
                
                // Contar invoice items
                long itemsCount = invoice.getItems() != null ? invoice.getItems().size() : 0;
                response.put("itemsCount", itemsCount);
                
                log.info("✅ TESTE: Invoice encontrada - Items: {}", itemsCount);
            } else {
                log.warn("⚠️ TESTE: Invoice não encontrada: {}", invoiceId);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ TESTE: Erro ao consultar invoice: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("invoiceId", invoiceId.toString());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Health check simples
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        log.info("🧪 TESTE: Health check");
        return ResponseEntity.ok("TESTE: Sistema funcionando!");
    }
} 