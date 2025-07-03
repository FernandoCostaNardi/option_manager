package com.olisystem.optionsmanager.controller.ocr;

import com.olisystem.optionsmanager.dto.invoice.InvoiceImportRequest;
import com.olisystem.optionsmanager.dto.invoice.InvoiceImportResponse;
import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.brokerage.Brokerage;
import com.olisystem.optionsmanager.repository.BrokerageRepository;
import com.olisystem.optionsmanager.service.auth.UserService;
import com.olisystem.optionsmanager.service.invoice.InvoiceImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * 🔍 Controller REAL para processamento de OCR e importação de notas
 * 
 * Integra extração de texto PDF com sistema de importação de invoices
 */
@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
@Slf4j
public class OcrController {

    private final InvoiceImportService invoiceImportService;
    private final UserService userService;
    private final BrokerageRepository brokerageRepository;

    /**
     * 📁 Upload de arquivo para processamento OCR + Importação automática
     * 
     * Fluxo completo:
     * 1. Upload do PDF
     * 2. Extração de texto via PDFBox
     * 3. Criação automática do InvoiceImportRequest
     * 4. Processamento via InvoiceImportService
     * 5. Retorna resultado da importação
     */
    @PostMapping("/upload")
    public ResponseEntity<OcrUploadResponse> uploadFile(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "brokerageId", required = false) String brokerageId,
            @RequestParam(value = "autoImport", defaultValue = "true") boolean autoImport,
            Authentication authentication) {
        
        log.info("🔍 OCR: Upload iniciado - file: {}, auto-import: {}, user: {}", 
                 file != null ? file.getOriginalFilename() : "null", autoImport, authentication.getName());
        
        try {
            // === VALIDAÇÕES BÁSICAS ===
            if (file == null) {
                log.warn("⚠️ Nenhum arquivo enviado");
                return ResponseEntity.badRequest()
                    .body(OcrUploadResponse.error("Nenhum arquivo foi enviado. Use o parâmetro 'file' com multipart/form-data"));
            }
            
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(OcrUploadResponse.error("Arquivo não pode estar vazio"));
            }
            
            if (file.getSize() > 10 * 1024 * 1024) { // 10MB limite
                return ResponseEntity.badRequest()
                    .body(OcrUploadResponse.error("Arquivo muito grande. Limite: 10MB"));
            }
            
            // Verificar tipo de arquivo
            String contentType = file.getContentType();
            if (contentType == null || !contentType.equals("application/pdf")) {
                return ResponseEntity.badRequest()
                    .body(OcrUploadResponse.error("Apenas arquivos PDF são suportados"));
            }
            
            log.info("📄 Arquivo válido: nome={}, tamanho={} bytes", 
                     file.getOriginalFilename(), file.getSize());
            
            // === EXTRAÇÃO DE TEXTO ===
            long startTime = System.currentTimeMillis();
            String extractedText = extractTextFromPdf(file);
            long extractionTime = System.currentTimeMillis() - startTime;
            
            log.info("✅ Texto extraído em {} ms - {} caracteres", 
                     extractionTime, extractedText.length());
            
            // === AUTO-IMPORTAÇÃO ===
            if (autoImport) {
                return processAutoImport(file, extractedText, brokerageId, authentication, extractionTime);
            } else {
                // Apenas OCR, sem importação
                return ResponseEntity.ok(OcrUploadResponse.success(
                    file.getOriginalFilename(),
                    file.getSize(),
                    contentType,
                    extractedText,
                    extractionTime + "ms"
                ));
            }
            
        } catch (Exception e) {
            log.error("💥 Erro no processamento OCR: {}", e.getMessage(), e);
            
            return ResponseEntity.status(500)
                .body(OcrUploadResponse.error("Erro interno: " + e.getMessage()));
        }
    }

    /**
     * 🔄 Processa auto-importação após extração de texto
     */
    private ResponseEntity<OcrUploadResponse> processAutoImport(
            MultipartFile file, 
            String extractedText, 
            String brokerageId, 
            Authentication authentication,
            long extractionTime) {
        
        try {
            log.info("🔄 Iniciando auto-importação...");
            
            // Obter usuário autenticado
            User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
            
            // 🔧 CORREÇÃO: Hash deve ser baseado no texto extraído (não no arquivo original)
            String fileHash = generateFileHash(extractedText);
            
            // Detectar corretora automaticamente se não fornecida
            UUID detectedBrokerageId = detectBrokerageId(extractedText, brokerageId);
            
            // Criar request de importação
            InvoiceImportRequest importRequest = new InvoiceImportRequest(
                detectedBrokerageId,
                List.of(new InvoiceImportRequest.InvoiceFileData(
                    file.getOriginalFilename(),
                    extractedText,  // 🔧 CORREÇÃO: usar texto extraído em vez de base64
                    fileHash
                ))
            );
            
            // Processar importação
            InvoiceImportResponse importResponse = invoiceImportService.importInvoices(importRequest, user);
            
            log.info("✅ Auto-importação concluída - Sucessos: {}, Erros: {}", 
                     importResponse.successfulImports(), importResponse.failedImports());
            
            // Preparar resposta combinada
            String message = String.format("OCR + Importação: %d sucessos, %d erros", 
                                          importResponse.successfulImports(), importResponse.failedImports());
            
            return ResponseEntity.ok(OcrUploadResponse.successWithImport(
                file.getOriginalFilename(),
                file.getSize(),
                file.getContentType(),
                extractedText,
                extractionTime + "ms",
                importResponse
            ));
            
        } catch (Exception e) {
            log.error("💥 Erro na auto-importação: {}", e.getMessage(), e);
            
            // Retornar OCR com erro de importação
            return ResponseEntity.ok(OcrUploadResponse.successWithImportError(
                file.getOriginalFilename(),
                file.getSize(),
                file.getContentType(),
                extractedText,
                extractionTime + "ms",
                "Erro na importação: " + e.getMessage()
            ));
        }
    }

    /**
     * 📄 Extrai texto de PDF usando PDFBox
     */
    private String extractTextFromPdf(MultipartFile file) throws IOException {
        log.debug("📄 Extraindo texto do PDF...");
        
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            
            // Configurações para melhor extração
            stripper.setSortByPosition(true);
            stripper.setLineSeparator("\n");
            
            String text = stripper.getText(document);
            
            log.debug("✅ Texto extraído: {} páginas, {} caracteres", 
                     document.getNumberOfPages(), text.length());
            
            return text;
        }
    }

    /**
     * 🔍 Detecta automaticamente a corretora baseada no texto extraído
     */
    private UUID detectBrokerageId(String text, String providedBrokerageId) {
        log.debug("🔍 Detectando corretora...");
        
        // Se fornecido manualmente, usar
        if (providedBrokerageId != null && !providedBrokerageId.trim().isEmpty()) {
            try {
                UUID brokerageUuid = UUID.fromString(providedBrokerageId);
                log.info("✅ Corretora fornecida manualmente: {}", brokerageUuid);
                return brokerageUuid;
            } catch (IllegalArgumentException e) {
                log.warn("⚠️ UUID de corretora inválido fornecido: {}", providedBrokerageId);
            }
        }
        
        // Mapear padrões de texto para CNPJs conhecidos
        String detectedCnpj = null;
        String detectedName = null;
        
        if (text.contains("BTG Pactual") || text.contains("BTG PACTUAL")) {
            detectedCnpj = "42.815.158/0001-22"; // CNPJ real da BTG Pactual
            detectedName = "BTG Pactual";
        } else if (text.contains("CLEAR") || text.contains("Clear Corretora") || text.contains("XP INVESTIMENTOS")) {
            detectedCnpj = "02.332.886/0001-04"; // CNPJ real da Clear/XP
            detectedName = "Clear";
        } else if (text.contains("RICO") || text.contains("Rico Investimentos")) {
            detectedCnpj = "03.509.645/0001-23"; // CNPJ real da Rico
            detectedName = "Rico";
        } else if (text.contains("TORO") || text.contains("Toro Investimentos")) {
            detectedCnpj = "26.563.455/0001-20"; // CNPJ real da Toro
            detectedName = "Toro";
        }
        
        if (detectedCnpj != null) {
            log.info("🔍 Corretora detectada: {} (CNPJ: {})", detectedName, detectedCnpj);
            
            // Buscar no banco pelo CNPJ
            Optional<Brokerage> brokerageOpt = brokerageRepository.findByCnpj(detectedCnpj);
            if (brokerageOpt.isPresent()) {
                UUID brokerageId = brokerageOpt.get().getId();
                log.info("✅ Corretora encontrada no banco: {} (ID: {})", detectedName, brokerageId);
                return brokerageId;
            } else {
                log.warn("⚠️ Corretora {} detectada mas não encontrada no banco (CNPJ: {})", detectedName, detectedCnpj);
                throw new RuntimeException(String.format(
                    "Corretora %s detectada mas não cadastrada no sistema. " +
                    "Por favor, cadastre a corretora com CNPJ %s ou forneça o brokerageId manualmente.", 
                    detectedName, detectedCnpj));
            }
        }
        
        log.warn("⚠️ Corretora não detectada automaticamente no texto");
        throw new RuntimeException("Corretora não detectada. Forneça o brokerageId manualmente ou " +
                                 "certifique-se de que o PDF contém informações reconhecíveis da corretora.");
    }

    /**
     * 🔐 Gera hash SHA-256 do conteúdo (compatível com InvoiceUtils)
     */
    private String generateFileHash(String content) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
        
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        
        return hexString.toString();
    }

    /**
     * 📊 Status do serviço OCR
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getOcrStatus() {
        log.debug("📊 Verificando status do serviço OCR");
        
        Map<String, Object> status = new HashMap<>();
        status.put("service", "OCR Service + Auto Import");
        status.put("status", "OPERATIONAL");
        status.put("version", "2.0.0");
        status.put("supportedFormats", new String[]{"PDF"});
        status.put("maxFileSize", "10MB");
        status.put("features", Arrays.asList("PDF Text Extraction", "Auto Import", "Duplicate Detection", "Multi-Brokerage Support"));
        status.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(status);
    }

    // === DTOs ===
    
    @lombok.Data
    @lombok.Builder
    public static class OcrUploadResponse {
        private boolean success;
        private String message;
        private String fileName;
        private Long fileSize;
        private String contentType;
        private String extractedText;
        private String processingTime;
        private String errorDetails;
        
        // Dados de importação (quando auto-import habilitado)
        private Boolean autoImportEnabled;
        private InvoiceImportResponse importResult;
        private String importError;
        
        public static OcrUploadResponse success(String fileName, Long fileSize, 
                                              String contentType, String extractedText, String processingTime) {
            return OcrUploadResponse.builder()
                .success(true)
                .message("OCR processado com sucesso")
                .fileName(fileName)
                .fileSize(fileSize)
                .contentType(contentType)
                .extractedText(extractedText)
                .processingTime(processingTime)
                .autoImportEnabled(false)
                .build();
        }
        
        public static OcrUploadResponse successWithImport(String fileName, Long fileSize, 
                                                        String contentType, String extractedText, 
                                                        String processingTime, InvoiceImportResponse importResult) {
            return OcrUploadResponse.builder()
                .success(true)
                .message("OCR + Importação processados com sucesso")
                .fileName(fileName)
                .fileSize(fileSize)
                .contentType(contentType)
                .extractedText(extractedText)
                .processingTime(processingTime)
                .autoImportEnabled(true)
                .importResult(importResult)
                .build();
        }
        
        public static OcrUploadResponse successWithImportError(String fileName, Long fileSize, 
                                                             String contentType, String extractedText, 
                                                             String processingTime, String importError) {
            return OcrUploadResponse.builder()
                .success(true)
                .message("OCR processado, mas houve erro na importação")
                .fileName(fileName)
                .fileSize(fileSize)
                .contentType(contentType)
                .extractedText(extractedText)
                .processingTime(processingTime)
                .autoImportEnabled(true)
                .importError(importError)
                .build();
        }
        
        public static OcrUploadResponse error(String errorMessage) {
            return OcrUploadResponse.builder()
                .success(false)
                .message("Erro no processamento")
                .errorDetails(errorMessage)
                .autoImportEnabled(false)
                .build();
        }
    }
}
