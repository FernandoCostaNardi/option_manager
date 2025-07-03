package com.olisystem.optionsmanager.repository;

import com.olisystem.optionsmanager.model.invoice.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    
    // Buscar por hash do arquivo para evitar duplicatas
    Optional<Invoice> findByFileHash(String fileHash);
    
    // Buscar por número da nota e corretora
    @Query("SELECT i FROM Invoice i WHERE i.invoiceNumber = :invoiceNumber AND i.brokerage.id = :brokerageId")
    Optional<Invoice> findByInvoiceNumberAndBrokerageId(@Param("invoiceNumber") String invoiceNumber, 
                                                       @Param("brokerageId") UUID brokerageId);
    
    // Buscar por usuário
    List<Invoice> findByUserIdOrderByTradingDateDesc(UUID userId);
    
    // Buscar por período
    @Query("SELECT i FROM Invoice i WHERE i.tradingDate BETWEEN :startDate AND :endDate ORDER BY i.tradingDate DESC")
    List<Invoice> findByTradingDateBetween(@Param("startDate") LocalDate startDate, 
                                          @Param("endDate") LocalDate endDate);
    
    // Buscar por corretora
    List<Invoice> findByBrokerageIdOrderByTradingDateDesc(UUID brokerageId);
    
    // Verificar se existe por hash (para validação rápida)
    boolean existsByFileHash(String fileHash);
    
    // TODO: Buscar notas não processadas (implementar na Fase 2 quando definir relacionamento)
    // @Query("SELECT i FROM Invoice i WHERE NOT EXISTS (SELECT 1 FROM Operation o WHERE o.sourceInvoiceId = i.id)")
    // List<Invoice> findUnprocessedInvoices();
    
    // Implementação temporária - retorna todas as invoices
    @Query("SELECT i FROM Invoice i ORDER BY i.tradingDate DESC")
    List<Invoice> findUnprocessedInvoices();
    
    // === MÉTODOS ADICIONADOS PARA SISTEMA DE IMPORTAÇÃO V2 ===
    
    /**
     * Busca notas por usuário com paginação
     */
    @Query("SELECT i FROM Invoice i WHERE i.user.id = :userId ORDER BY i.tradingDate DESC")
    Page<Invoice> findByUser(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Busca notas por corretora e usuário com paginação
     */
    @Query("SELECT i FROM Invoice i WHERE i.brokerage.id = :brokerageId AND i.user.id = :userId")
    Page<Invoice> findByBrokerageAndUser(
        @Param("brokerageId") UUID brokerageId,
        @Param("userId") UUID userId,
        Pageable pageable
    );

    /**
     * Busca notas por usuário e período
     */
    @Query("SELECT i FROM Invoice i WHERE i.user.id = :userId " +
           "AND i.tradingDate BETWEEN :startDate AND :endDate " +
           "ORDER BY i.tradingDate DESC")
    Page<Invoice> findByUserAndDateRange(
        @Param("userId") UUID userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        Pageable pageable
    );

    /**
     * Busca notas importadas em uma data específica
     */
    @Query("SELECT i FROM Invoice i WHERE i.user.id = :userId " +
           "AND DATE(i.importedAt) = :importDate " +
           "ORDER BY i.importedAt DESC")
    List<Invoice> findByUserAndImportDate(
        @Param("userId") UUID userId,
        @Param("importDate") LocalDate importDate
    );

    /**
     * Busca últimas notas importadas
     */
    @Query("SELECT i FROM Invoice i WHERE i.user.id = :userId " +
           "ORDER BY i.importedAt DESC")
    List<Invoice> findLatestImportedByUser(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Conta total de notas por usuário
     */
    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.user.id = :userId")
    Long countByUser(@Param("userId") UUID userId);

    /**
     * Conta notas por corretora e usuário
     */
    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.brokerage.id = :brokerageId AND i.user.id = :userId")
    Long countByBrokerageAndUser(
        @Param("brokerageId") UUID brokerageId,
        @Param("userId") UUID userId
    );
}
