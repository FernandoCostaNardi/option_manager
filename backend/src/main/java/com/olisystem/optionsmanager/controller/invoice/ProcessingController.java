package com.olisystem.optionsmanager.controller.invoice;

import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import com.olisystem.optionsmanager.repository.InvoiceItemRepository;
import com.olisystem.optionsmanager.repository.InvoiceRepository;
import com.olisystem.optionsmanager.service.auth.UserService;
import com.olisystem.optionsmanager.service.invoice.processing.RealInvoiceProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller para processamento de invoices
 * ✅ MELHORADO: Estimativa baseada no conteúdo real das invoices
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-18
 */
@RestController
@RequestMapping("/api/processing")
@RequiredArgsConstructor
@Slf4j
public class ProcessingController {

    private final RealInvoiceProcessor realInvoiceProcessor;
    private final UserService userService;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;

    /**
     * ✅ MELHORADO: Endpoint para estimar processamento de invoices baseado no conteúdo real
     */
    @PostMapping("/estimate")
    public ResponseEntity<Map<String, Object>> estimateProcessing(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        log.info("📊 Estimando processamento de invoices (versão melhorada)...");
        
        try {
            // ✅ CORREÇÃO: Usar usuário padrão se não houver autenticação
            User currentUser;
            if (authentication != null && authentication.isAuthenticated()) {
                currentUser = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + authentication.getName()));
            } else {
                // ✅ TEMPORÁRIO: Usar usuário padrão para testes
                currentUser = userService.findByUsername("admin")
                    .orElseGet(() -> {
                        log.warn("⚠️ Usuário admin não encontrado, criando usuário temporário");
                        User tempUser = new User();
                        tempUser.setId(UUID.randomUUID());
                        tempUser.setUsername("temp-user");
                        tempUser.setEmail("temp@example.com");
                        return tempUser;
                    });
            }
            
            @SuppressWarnings("unchecked")
            List<String> invoiceIds = (List<String>) request.get("invoiceIds");
            
            if (invoiceIds == null || invoiceIds.isEmpty()) {
                log.warn("⚠️ Nenhuma invoice fornecida para estimativa");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Nenhuma invoice fornecida");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Converter strings para UUIDs
            List<UUID> uuidInvoiceIds = invoiceIds.stream()
                .map(UUID::fromString)
                .toList();
            
            log.info("📊 Estimando processamento para {} invoices", uuidInvoiceIds.size());
            
            // ✅ MELHORADO: Calcular estimativa baseada no conteúdo real das invoices
            int totalInvoices = uuidInvoiceIds.size();
            int totalItems = 0;
            int totalOperations = 0;
            long totalEstimatedTimeMs = 0;
            
            // Analisar cada invoice para calcular estimativa real
            for (UUID invoiceId : uuidInvoiceIds) {
                try {
                    Invoice invoice = invoiceRepository.findById(invoiceId)
                        .orElseThrow(() -> new RuntimeException("Invoice não encontrada: " + invoiceId));
                    
                    // Buscar items da invoice
                    List<InvoiceItem> items = invoiceItemRepository.findByInvoiceIdOrderBySequenceNumber(invoiceId);
                    int itemCount = items.size();
                    totalItems += itemCount;
                    
                    // Calcular operações baseadas nos items
                    int operationsForInvoice = calculateOperationsForInvoice(items);
                    totalOperations += operationsForInvoice;
                    
                    // Calcular tempo estimado baseado na complexidade dos items
                    long timeForInvoice = calculateTimeForInvoice(items);
                    totalEstimatedTimeMs += timeForInvoice;
                    
                    log.debug("📋 Invoice {}: {} items, {} operações, {}ms", 
                        invoice.getInvoiceNumber(), itemCount, operationsForInvoice, timeForInvoice);
                    
                } catch (Exception e) {
                    log.warn("⚠️ Erro ao analisar invoice {}: {}", invoiceId, e.getMessage());
                    // Usar estimativa padrão para invoices com erro
                    totalItems += 3; // Estimativa padrão
                    totalOperations += 3;
                    totalEstimatedTimeMs += 2500; // 2.5 segundos padrão
                }
            }
            
            // ✅ MELHORADO: Calcular complexidade baseada no conteúdo real
            String complexity = calculateComplexity(totalInvoices, totalItems, totalOperations);
            
            // Formatar tempo estimado (mm:ss)
            int estimatedTimeSeconds = (int) (totalEstimatedTimeMs / 1000);
            String estimatedTimeFormatted = String.format("%02d:%02d", estimatedTimeSeconds / 60, estimatedTimeSeconds % 60);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalInvoices", totalInvoices);
            response.put("totalItems", totalItems);
            response.put("estimatedOperations", totalOperations);
            response.put("complexity", complexity);
            response.put("estimatedTimeMs", totalEstimatedTimeMs);
            response.put("estimatedTimeSeconds", estimatedTimeSeconds);
            response.put("estimatedTimeFormatted", estimatedTimeFormatted);
            response.put("estimatedRemainingTime", totalEstimatedTimeMs);
            response.put("message", String.format("Estimativa: %d invoice(s) com %d items em ~%d segundos", 
                totalInvoices, totalItems, estimatedTimeSeconds));
            
            log.info("✅ Estimativa calculada: {} invoices, {} items, {} operações, {}ms, complexidade {}", 
                totalInvoices, totalItems, totalOperations, totalEstimatedTimeMs, complexity);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("❌ IDs de invoice inválidos: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "IDs de invoice inválidos: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            log.error("❌ Erro ao estimar processamento: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Erro interno: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * ✅ CORRIGIDO: Calcular número de operações baseado nos items da invoice
     * Cada item gera exatamente 1 operação, independente do tipo
     */
    private int calculateOperationsForInvoice(List<InvoiceItem> items) {
        // ✅ CORREÇÃO: Cada item gera exatamente 1 operação
        // Não há multiplicação baseada no tipo de mercado
        int operations = items.size();
        
        log.debug("📊 Calculando operações: {} items = {} operações", items.size(), operations);
        
        return Math.max(operations, 1); // Mínimo 1 operação
    }

    /**
     * ✅ NOVO: Calcular tempo estimado baseado na complexidade dos items
     */
    private long calculateTimeForInvoice(List<InvoiceItem> items) {
        long baseTime = 1000; // 1 segundo base
        long timePerItem = 500; // 0.5 segundos por item
        
        // Tempo base + tempo por item
        long totalTime = baseTime + (items.size() * timePerItem);
        
        // Adicionar tempo extra para items complexos
        for (InvoiceItem item : items) {
            String marketType = item.getMarketType();
            if (marketType != null && marketType.contains("OPCAO")) {
                totalTime += 300; // 0.3 segundos extra para opções
            }
        }
        
        return totalTime;
    }

    /**
     * ✅ NOVO: Calcular complexidade baseada no conteúdo real
     */
    private String calculateComplexity(int totalInvoices, int totalItems, int totalOperations) {
        // Complexidade baseada em múltiplos fatores
        int complexityScore = 0;
        
        // Fator 1: Número de invoices
        if (totalInvoices <= 2) complexityScore += 1;
        else if (totalInvoices <= 5) complexityScore += 2;
        else complexityScore += 3;
        
        // Fator 2: Número de items
        if (totalItems <= 10) complexityScore += 1;
        else if (totalItems <= 25) complexityScore += 2;
        else complexityScore += 3;
        
        // Fator 3: Número de operações
        if (totalOperations <= 15) complexityScore += 1;
        else if (totalOperations <= 35) complexityScore += 2;
        else complexityScore += 3;
        
        // Determinar complexidade baseada no score
        if (complexityScore <= 3) return "BAIXA";
        else if (complexityScore <= 6) return "MÉDIA";
        else return "ALTA";
    }

    /**
     * ✅ NOVO: Endpoint para processar invoices (redirecionamento para o controller real)
     */
    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processInvoices(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        log.info("🚀 Processando invoices via endpoint /api/processing/process");
        
        // Redirecionar para o controller real
        // Por enquanto, retornar uma resposta informando que deve usar o endpoint correto
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Use o endpoint /api/invoice/processing/real/process para processamento");
        response.put("correctEndpoint", "/api/invoice/processing/real/process");
        
        return ResponseEntity.ok(response);
    }

    /**
     * ✅ NOVO: Endpoint para status de processamento
     */
    @GetMapping("/status/{sessionId}")
    public ResponseEntity<Map<String, Object>> getProcessingStatus(@PathVariable String sessionId) {
        
        log.info("📊 Consultando status de processamento: {}", sessionId);
        
        // Por enquanto, retornar uma resposta informando que deve usar o endpoint correto
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Use o endpoint /api/invoice/processing/real/status/{sessionId} para consultar status");
        response.put("correctEndpoint", "/api/invoice/processing/real/status/" + sessionId);
        
        return ResponseEntity.ok(response);
    }
} 