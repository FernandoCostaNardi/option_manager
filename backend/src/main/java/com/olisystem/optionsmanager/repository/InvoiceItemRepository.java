package com.olisystem.optionsmanager.repository;

import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, UUID> {
    
    // Buscar itens por invoice
    List<InvoiceItem> findByInvoiceIdOrderBySequenceNumber(UUID invoiceId);
    
    // Buscar operações de Day Trade
    List<InvoiceItem> findByIsDayTradeTrue();
    
    // Buscar por tipo de operação
    List<InvoiceItem> findByOperationType(String operationType);
    
    // Buscar por código do ativo
    List<InvoiceItem> findByAssetCodeOrderByInvoice_TradingDateDesc(String assetCode);
    
    // Estatísticas por ativo
    @Query("SELECT ii.assetCode, COUNT(ii), SUM(ii.totalValue) FROM InvoiceItem ii GROUP BY ii.assetCode ORDER BY COUNT(ii) DESC")
    List<Object[]> getAssetStatistics();
    
    // Buscar operações por período e ativo
    @Query("SELECT ii FROM InvoiceItem ii WHERE ii.assetCode = :assetCode AND ii.invoice.tradingDate BETWEEN :startDate AND :endDate ORDER BY ii.invoice.tradingDate")
    List<InvoiceItem> findByAssetCodeAndDateRange(@Param("assetCode") String assetCode, 
                                                  @Param("startDate") java.time.LocalDate startDate,
                                                  @Param("endDate") java.time.LocalDate endDate);
}
