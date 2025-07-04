package com.olisystem.optionsmanager.repository;

import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationSourceMapping;
import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import com.olisystem.optionsmanager.model.enums.OperationMappingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para OperationSourceMapping
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-03
 */
@Repository
public interface OperationSourceMappingRepository extends JpaRepository<OperationSourceMapping, UUID> {
    
    /**
     * Busca mapeamentos por operação
     */
    List<OperationSourceMapping> findByOperationOrderByProcessingSequence(Operation operation);
    
    /**
     * Busca mapeamentos por invoice
     */
    List<OperationSourceMapping> findByInvoiceOrderByProcessingSequence(Invoice invoice);
    
    /**
     * Busca mapeamento por invoice item (deveria ser único)
     */
    Optional<OperationSourceMapping> findByInvoiceItem(InvoiceItem invoiceItem);
    
    /**
     * Busca mapeamentos por tipo
     */
    List<OperationSourceMapping> findByMappingTypeOrderByCreatedAtDesc(OperationMappingType mappingType);
    
    /**
     * Busca mapeamentos de uma invoice por tipo
     */
    List<OperationSourceMapping> findByInvoiceAndMappingTypeOrderByProcessingSequence(Invoice invoice, 
                                                                                     OperationMappingType mappingType);
    
    /**
     * Verifica se já existe mapeamento para um invoice item
     */
    boolean existsByInvoiceItem(InvoiceItem invoiceItem);
    
    /**
     * Busca operations criadas a partir de uma invoice
     */
    @Query("SELECT DISTINCT osm.operation FROM OperationSourceMapping osm WHERE osm.invoice = :invoice")
    List<Operation> findOperationsByInvoice(@Param("invoice") Invoice invoice);
    
    /**
     * Busca invoice items que originaram uma operation
     */
    @Query("SELECT osm.invoiceItem FROM OperationSourceMapping osm WHERE osm.operation = :operation ORDER BY osm.processingSequence")
    List<InvoiceItem> findInvoiceItemsByOperation(@Param("operation") Operation operation);
    
    /**
     * Conta mapeamentos por tipo para uma invoice
     */
    @Query("SELECT osm.mappingType, COUNT(osm) FROM OperationSourceMapping osm WHERE osm.invoice = :invoice GROUP BY osm.mappingType")
    List<Object[]> countByMappingTypeForInvoice(@Param("invoice") Invoice invoice);
    
    /**
     * Busca mapeamentos Day Trade de uma invoice
     */
    @Query("SELECT osm FROM OperationSourceMapping osm WHERE osm.invoice = :invoice AND osm.mappingType IN ('DAY_TRADE_ENTRY', 'DAY_TRADE_EXIT') ORDER BY osm.processingSequence")
    List<OperationSourceMapping> findDayTradeMappings(@Param("invoice") Invoice invoice);
    
    /**
     * Busca últimos mapeamentos criados para debugging
     */
    @Query("SELECT osm FROM OperationSourceMapping osm ORDER BY osm.createdAt DESC")
    List<OperationSourceMapping> findLatestMappings(org.springframework.data.domain.Pageable pageable);
}