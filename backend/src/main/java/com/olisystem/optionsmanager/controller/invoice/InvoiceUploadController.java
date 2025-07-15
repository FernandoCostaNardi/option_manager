package com.olisystem.optionsmanager.controller.invoice;

import com.olisystem.optionsmanager.dto.invoice.*;
import com.olisystem.optionsmanager.model.auth.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Controller REST para upload de invoices
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@RestController
@RequestMapping("/api/v1/invoices/upload")
@RequiredArgsConstructor
@Slf4j
public class InvoiceUploadController {

    /**
     * Upload de arquivo único
     */
    @PostMapping("/file")
    public ResponseEntity<InvoiceUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) {
        
        log.info("📁 Upload de arquivo: {} (User: {})", file.getOriginalFilename(), user.getEmail());
        
        try {
            // TODO: Implementar processamento do arquivo
            // Por enquanto, retornamos uma resposta mock
            
            InvoiceUploadResponse response = InvoiceUploadResponse.builder()
                .uploadId(UUID.randomUUID())
                .status("UPLOADED")
                .message("Arquivo recebido com sucesso")
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .build();
            
            log.info("✅ Upload concluído - UploadId: {}", response.getUploadId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Erro no upload: {}", e.getMessage(), e);
            
            InvoiceUploadResponse response = InvoiceUploadResponse.builder()
                .status("ERROR")
                .errorMessage("Erro no upload: " + e.getMessage())
                .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Upload de múltiplos arquivos
     */
    @PostMapping("/files")
    public ResponseEntity<InvoiceUploadResponse> uploadFiles(
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal User user) {
        
        log.info("📁 Upload de {} arquivos (User: {})", files.size(), user.getEmail());
        
        try {
            // TODO: Implementar processamento dos arquivos
            // Por enquanto, retornamos uma resposta mock
            
            InvoiceUploadResponse response = InvoiceUploadResponse.builder()
                .uploadId(UUID.randomUUID())
                .status("UPLOADED")
                .message(String.format("%d arquivos recebidos com sucesso", files.size()))
                .totalFiles(files.size())
                .totalSize(files.stream().mapToLong(MultipartFile::getSize).sum())
                .build();
            
            log.info("✅ Upload de múltiplos arquivos concluído - UploadId: {}", response.getUploadId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Erro no upload de múltiplos arquivos: {}", e.getMessage(), e);
            
            InvoiceUploadResponse response = InvoiceUploadResponse.builder()
                .status("ERROR")
                .errorMessage("Erro no upload: " + e.getMessage())
                .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Consulta status de upload
     */
    @GetMapping("/status/{uploadId}")
    public ResponseEntity<InvoiceUploadStatusResponse> getUploadStatus(@PathVariable UUID uploadId) {
        
        log.debug("📊 Consultando status do upload: {}", uploadId);
        
        // TODO: Implementar consulta real do status
        // Por enquanto, retornamos uma resposta mock
        
        InvoiceUploadStatusResponse response = InvoiceUploadStatusResponse.builder()
            .uploadId(uploadId)
            .status("PROCESSED")
            .message("Upload processado com sucesso")
            .progress(100)
            .build();
        
        return ResponseEntity.ok(response);
    }

    /**
     * Lista uploads do usuário
     */
    @GetMapping("/list")
    public ResponseEntity<List<InvoiceUploadSummaryResponse>> listUploads(@AuthenticationPrincipal User user) {
        
        log.debug("📋 Listando uploads do usuário: {}", user.getEmail());
        
        // TODO: Implementar listagem real
        // Por enquanto, retornamos uma lista vazia
        
        List<InvoiceUploadSummaryResponse> uploads = List.of();
        
        return ResponseEntity.ok(uploads);
    }
} 