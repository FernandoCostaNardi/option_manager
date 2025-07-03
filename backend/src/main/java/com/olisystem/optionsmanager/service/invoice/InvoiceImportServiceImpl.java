package com.olisystem.optionsmanager.service.invoice;

import com.olisystem.optionsmanager.dto.invoice.InvoiceImportRequest;
import com.olisystem.optionsmanager.dto.invoice.InvoiceImportResponse;
import com.olisystem.optionsmanager.dto.invoice.InvoiceData;
import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.brokerage.Brokerage;
import com.olisystem.optionsmanager.repository.InvoiceRepository;
import com.olisystem.optionsmanager.repository.BrokerageRepository;
import com.olisystem.optionsmanager.service.invoice.parser.InvoiceParserService;
import com.olisystem.optionsmanager.service.invoice.mapper.InvoiceMapperService;
import com.olisystem.optionsmanager.validator.invoice.InvoiceFileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementação do serviço de importação de notas de corretagem
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InvoiceImportServiceImpl implements InvoiceImportService {

    private final InvoiceRepository invoiceRepository;
    private final BrokerageRepository brokerageRepository;
    private final InvoiceParserService invoiceParserService;
    private final InvoiceMapperService invoiceMapperService;
    private final InvoiceFileValidator invoiceFileValidator;

    @Override
    public InvoiceImportResponse importInvoices(InvoiceImportRequest request, User user) {
        log.info("=== INICIANDO IMPORTAÇÃO DE NOTAS ===");
        log.info("Usuário: {}, Corretora: {}, Arquivos: {}", 
                 user.getUsername(), request.brokerageId(), request.files().size());

        try {
            // 🔧 VALIDAÇÃO COMPLETA DA REQUISIÇÃO
            invoiceFileValidator.validateImportRequest(request);
            log.info("✅ Validação da requisição concluída");
        } catch (Exception e) {
            log.error("❌ Falha na validação: {}", e.getMessage());
            
            return new InvoiceImportResponse(
                "Falha na validação: " + e.getMessage(),
                request.files().size(),
                0,
                0,
                request.files().size(),
                LocalDateTime.now(),
                request.files().stream()
                    .map(file -> new InvoiceImportResponse.ImportResult(
                        file.fileName(),
                        InvoiceImportResponse.ImportStatus.ERROR,
                        "Validação falhou: " + e.getMessage(),
                        null,
                        null
                    ))
                    .toList()
            );
        }

        // Contadores para estatísticas
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger duplicateCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        List<InvoiceImportResponse.ImportResult> results = new ArrayList<>();

        // Processa cada arquivo
        for (InvoiceImportRequest.InvoiceFileData fileData : request.files()) {
            try {
                log.info("Processando arquivo: {}", fileData.fileName());
                
                InvoiceImportResponse.ImportResult result = processSingleFile(fileData, request, user);
                results.add(result);
                
                // Atualiza contadores
                switch (result.status()) {
                    case SUCCESS -> successCount.incrementAndGet();
                    case DUPLICATE -> duplicateCount.incrementAndGet();
                    case ERROR -> errorCount.incrementAndGet();
                }
                
            } catch (Exception e) {
                log.error("Erro inesperado ao processar arquivo {}: {}", fileData.fileName(), e.getMessage(), e);
                
                results.add(new InvoiceImportResponse.ImportResult(
                    fileData.fileName(),
                    InvoiceImportResponse.ImportStatus.ERROR,
                    "Erro inesperado: " + e.getMessage(),
                    null,
                    null
                ));
                
                errorCount.incrementAndGet();
            }
        }

        log.info("=== IMPORTAÇÃO CONCLUÍDA ===");
        log.info("Total: {}, Sucessos: {}, Duplicados: {}, Erros: {}", 
                 request.files().size(), successCount.get(), duplicateCount.get(), errorCount.get());

        return new InvoiceImportResponse(
            "Importação concluída",
            request.files().size(),
            successCount.get(),
            duplicateCount.get(),
            errorCount.get(),
            LocalDateTime.now(),
            results
        );
    }

    @Override
    public boolean isFileAlreadyImported(String fileHash) {
        return invoiceRepository.existsByFileHash(fileHash);
    }

    @Override
    public InvoiceImportResponse.ImportResult processSingleFile(
            InvoiceImportRequest.InvoiceFileData fileData,
            InvoiceImportRequest request,
            User user) {
        
        try {
            // 1. Verificar se arquivo já foi importado
            if (isFileAlreadyImported(fileData.fileHash())) {
                log.warn("Arquivo {} já foi importado anteriormente", fileData.fileName());
                return new InvoiceImportResponse.ImportResult(
                    fileData.fileName(),
                    InvoiceImportResponse.ImportStatus.DUPLICATE,
                    "Arquivo já importado anteriormente",
                    null,
                    null
                );
            }

            // 2. Buscar corretora
            Brokerage brokerage = brokerageRepository.findById(request.brokerageId())
                .orElseThrow(() -> new IllegalArgumentException("Corretora não encontrada"));

            // 3. Extrair dados da nota usando parser
            Invoice parsedInvoice = invoiceParserService.parseInvoice(fileData, brokerage, user);

            // 4. Salvar no banco
            Invoice savedInvoice = invoiceRepository.save(parsedInvoice);

            // 5. Converter para DTO
            InvoiceData invoiceData = invoiceMapperService.toInvoiceData(savedInvoice);

            log.info("✅ Arquivo {} importado com sucesso. ID: {}", 
                     fileData.fileName(), savedInvoice.getId());

            return new InvoiceImportResponse.ImportResult(
                fileData.fileName(),
                InvoiceImportResponse.ImportStatus.SUCCESS,
                "Importado com sucesso",
                savedInvoice.getId(),
                invoiceData
            );

        } catch (Exception e) {
            log.error("Erro ao processar arquivo {}: {}", fileData.fileName(), e.getMessage(), e);
            
            return new InvoiceImportResponse.ImportResult(
                fileData.fileName(),
                InvoiceImportResponse.ImportStatus.ERROR,
                "Erro na importação: " + e.getMessage(),
                null,
                null
            );
        }
    }
}
