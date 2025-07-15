package com.olisystem.optionsmanager.controller.invoice;

/*
 * Controller REST para processamento de invoices - TEMPORARIAMENTE COMENTADO
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
/*
import com.olisystem.optionsmanager.dto.invoice.*;
import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.service.invoice.processing.orchestrator.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/invoices/processing")
@RequiredArgsConstructor
@Slf4j
public class InvoiceProcessingController {

    private final InvoiceProcessingOrchestrator orchestrator;
    private final TransactionManager transactionManager;
    private final ErrorHandler errorHandler;

    // Cache para acompanhar progresso em tempo real
    private final ConcurrentHashMap<UUID, ProcessingSession> activeSessions = new ConcurrentHashMap<>();

    // Métodos comentados temporariamente...
}
*/ 