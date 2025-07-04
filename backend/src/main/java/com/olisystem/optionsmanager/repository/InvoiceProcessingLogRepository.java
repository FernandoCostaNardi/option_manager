package com.olisystem.optionsmanager.repository;

import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.invoice.InvoiceProcessingLog;
import com.olisystem.optionsmanager.model.enums.InvoiceProcessingStatus;
import com.olisystem.optionsmanager.model.auth.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para InvoiceProcessingLog
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-03
 */
@Repository
public interface InvoiceProcessingLogRepository extends JpaRepository<InvoiceProcessingLog, UUID> {
    
    /**
     * Busca log de processamento por invoice
     */
    Optional<InvoiceProcessingLog> findByInvoice(Invoice invoice);
    
    /**
     * Busca log de processamento por invoice ID
     */
    Optional<InvoiceProcessingLog> findByInvoiceId(UUID invoiceId);
    
    /**
     * Busca todos os logs de um usuário
     */
    List<InvoiceProcessingLog> findByUserOrderByCreatedAtDesc(User user);
    
    /**
     * Busca logs por status
     */
    List<InvoiceProcessingLog> findByStatusOrderByCreatedAtDesc(InvoiceProcessingStatus status);
    
    /**
     * Busca logs por status e usuário
     */
    List<InvoiceProcessingLog> findByStatusAndUserOrderByCreatedAtDesc(InvoiceProcessingStatus status, User user);
    
    /**
     * Busca logs pendentes ou em processamento
     */
    @Query("SELECT ipl FROM InvoiceProcessingLog ipl WHERE ipl.status IN ('PENDING', 'PROCESSING') ORDER BY ipl.createdAt ASC")
    List<InvoiceProcessingLog> findActiveProcessing();
    
    /**
     * Busca logs com paginação e filtros
     */
    @Query("SELECT ipl FROM InvoiceProcessingLog ipl WHERE " +
           "(:userId IS NULL OR ipl.user.id = :userId) AND " +
           "(:status IS NULL OR ipl.status = :status) AND " +
           "(:startDate IS NULL OR ipl.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR ipl.createdAt <= :endDate) " +
           "ORDER BY ipl.createdAt DESC")
    Page<InvoiceProcessingLog> findWithFilters(@Param("userId") UUID userId,
                                              @Param("status") InvoiceProcessingStatus status,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate,
                                              Pageable pageable);
    
    /**
     * Conta logs por status para dashboard
     */
    @Query("SELECT ipl.status, COUNT(ipl) FROM InvoiceProcessingLog ipl WHERE ipl.user = :user GROUP BY ipl.status")
    List<Object[]> countByStatusForUser(@Param("user") User user);
    
    /**
     * Verifica se existe processamento ativo para uma invoice
     */
    @Query("SELECT COUNT(ipl) > 0 FROM InvoiceProcessingLog ipl WHERE ipl.invoice = :invoice AND ipl.status = 'PROCESSING'")
    boolean hasActiveProcessing(@Param("invoice") Invoice invoice);
}