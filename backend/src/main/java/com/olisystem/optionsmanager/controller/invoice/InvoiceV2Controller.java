package com.olisystem.optionsmanager.controller.invoice;

import com.olisystem.optionsmanager.dto.invoice.*;
import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.enums.InvoiceProcessingStatus;
import com.olisystem.optionsmanager.service.invoice.InvoiceImportService;
import com.olisystem.optionsmanager.service.invoice.InvoiceQueryService;
import com.olisystem.optionsmanager.service.auth.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Controller para importação e consulta de notas de corretagem
 * Novo sistema de importação (Fase 1)
 * ✅ ATUALIZADO: Suporte a filtro por status de processamento
 */
@RestController
@RequestMapping("/api/invoices-v2")
@RequiredArgsConstructor
@Slf4j
public class InvoiceV2Controller {

    private final InvoiceImportService invoiceImportService;
    private final InvoiceQueryService invoiceQueryService;
    private final UserService userService;

    /**
     * Importa múltiplas notas de corretagem
     * POST /api/invoices/import
     */
    @PostMapping("/import")
    public ResponseEntity<InvoiceImportResponse> importInvoices(
            @Valid @RequestBody InvoiceImportRequest request,
            Authentication authentication) {
        
        log.info("=== RECEBENDO REQUEST DE IMPORTAÇÃO ===");
        log.info("Usuário: {}, Corretora: {}, Arquivos: {}", 
                 authentication.getName(), request.brokerageId(), request.files().size());

        try {
            // Obter usuário autenticado
            User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
            
            // Processar importação
            InvoiceImportResponse response = invoiceImportService.importInvoices(request, user);
            
            log.info("✅ Importação concluída - Sucessos: {}, Erros: {}", 
                     response.successfulImports(), response.failedImports());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Erro na importação: {}", e.getMessage(), e);
            
            InvoiceImportResponse errorResponse = new InvoiceImportResponse(
                "Erro na importação: " + e.getMessage(),
                request.files().size(),
                0,
                0,
                request.files().size(),
                java.time.LocalDateTime.now(),
                List.of()
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Lista notas com filtros e paginação
     * GET /api/invoices-v2?page=0&size=1000&processingStatus=PENDING
     * ✅ ATUALIZADO: Suporte a filtro por status de processamento
     */
    @GetMapping
    public ResponseEntity<Page<InvoiceData>> getInvoices(
            @RequestParam(required = false) UUID brokerageId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String invoiceNumber,
            @RequestParam(required = false) String clientName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate importStartDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate importEndDate,
            @RequestParam(required = false) String processingStatus, // ✅ NOVO: Status de processamento
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            Authentication authentication) {
        
        log.info("📋 Buscando invoices com filtros - Status: {}, Page: {}, Size: {}", 
            processingStatus, page, size);
        
        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        InvoiceFilterRequest filterRequest = new InvoiceFilterRequest(
            brokerageId,
            startDate,
            endDate,
            invoiceNumber,
            clientName,
            importStartDate,
            importEndDate,
            processingStatus, // ✅ NOVO: Status de processamento
            page,
            size,
            sortBy,
            sortDirection
        );
        
        // ✅ NOVO: Usar método com suporte ao filtro ALL
        Page<InvoiceData> invoices = invoiceQueryService.findInvoicesWithProcessingStatusFilter(filterRequest, user);
        
        log.info("✅ Encontradas {} invoices", invoices.getTotalElements());
        return ResponseEntity.ok(invoices);
    }

    /**
     * Busca nota específica por ID
     * GET /api/invoices/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceData> getInvoiceById(
            @PathVariable UUID id,
            Authentication authentication) {
        
        log.debug("Buscando nota {} para usuário {}", id, authentication.getName());

        try {
            // Obter usuário autenticado
            User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
            
            // Buscar nota
            return invoiceQueryService.findInvoiceById(id, user)
                .map(invoice -> {
                    log.debug("✅ Nota encontrada: {}", invoice.invoiceNumber());
                    return ResponseEntity.ok(invoice);
                })
                .orElseGet(() -> {
                    log.warn("❌ Nota {} não encontrada", id);
                    return ResponseEntity.notFound().build();
                });
                
        } catch (Exception e) {
            log.error("❌ Erro ao buscar nota {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Lista notas por corretora
     * GET /api/invoices/brokerage/{brokerageId}
     */
    @GetMapping("/brokerage/{brokerageId}")
    public ResponseEntity<Page<InvoiceData>> getInvoicesByBrokerage(
            @PathVariable UUID brokerageId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        log.debug("Buscando notas da corretora {} para usuário {}", brokerageId, authentication.getName());

        try {
            // Obter usuário autenticado
            User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
            
            // Buscar notas
            Page<InvoiceData> invoices = invoiceQueryService.findInvoicesByBrokerage(brokerageId, user, page, size);
            
            log.debug("✅ Encontradas {} notas da corretora", invoices.getNumberOfElements());
            
            return ResponseEntity.ok(invoices);
            
        } catch (Exception e) {
            log.error("❌ Erro ao buscar notas da corretora {}: {}", brokerageId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Busca últimas notas importadas
     * GET /api/invoices/latest
     */
    @GetMapping("/latest")
    public ResponseEntity<List<InvoiceData>> getLatestInvoices(
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {
        
        log.debug("Buscando últimas {} notas para usuário {}", limit, authentication.getName());

        try {
            // Obter usuário autenticado
            User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
            
            // Buscar últimas notas
            List<InvoiceData> invoices = invoiceQueryService.findLatestImportedInvoices(user, limit);
            
            log.debug("✅ Encontradas {} notas recentes", invoices.size());
            
            return ResponseEntity.ok(invoices);
            
        } catch (Exception e) {
            log.error("❌ Erro ao buscar últimas notas: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Conta total de notas do usuário
     * GET /api/invoices/count
     * ✅ ATUALIZADO: Conta apenas notas não processadas por padrão
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countInvoices(Authentication authentication) {
        
        try {
            // Obter usuário autenticado
            User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
            
            // Contar notas não processadas
            Long count = invoiceQueryService.countInvoicesByUser(user);
            
            log.debug("✅ Usuário {} possui {} notas não processadas", authentication.getName(), count);
            
            return ResponseEntity.ok(count);
            
        } catch (Exception e) {
            log.error("❌ Erro ao contar notas: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * ✅ NOVO: Conta notas pendentes do usuário
     * GET /api/invoices-v2/count/pending
     */
    @GetMapping("/count/pending")
    public ResponseEntity<Long> countPendingInvoices(Authentication authentication) {
        
        try {
            // Obter usuário autenticado
            User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
            
            // Contar notas pendentes
            Long count = invoiceQueryService.countPendingInvoicesByUser(user);
            
            log.debug("✅ Usuário {} possui {} notas pendentes", authentication.getName(), count);
            
            return ResponseEntity.ok(count);
            
        } catch (Exception e) {
            log.error("❌ Erro ao contar notas pendentes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
