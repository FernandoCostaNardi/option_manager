package com.olisystem.optionsmanager.model.operation;

import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import com.olisystem.optionsmanager.model.enums.OperationMappingType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity para mapeamento entre Operations e InvoiceItems
 * Rastreia qual InvoiceItem originou qual Operation
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-03
 */
@Entity
@Table(name = "operation_source_mapping")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationSourceMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_id", nullable = false)
    private Operation operation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_item_id", nullable = false)
    private InvoiceItem invoiceItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "mapping_type", nullable = false)
    private OperationMappingType mappingType;

    @Column(name = "processing_sequence")
    private Integer processingSequence;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * Atualiza o timestamp de atualização automaticamente
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Cria um mapeamento para nova operação
     */
    public static OperationSourceMapping forNewOperation(Operation operation, InvoiceItem invoiceItem, 
                                                        Integer sequence) {
        return OperationSourceMapping.builder()
                .operation(operation)
                .invoice(invoiceItem.getInvoice())
                .invoiceItem(invoiceItem)
                .mappingType(OperationMappingType.NEW_OPERATION)
                .processingSequence(sequence)
                .notes("Nova operação criada a partir de invoice item")
                .build();
    }

    /**
     * Cria um mapeamento para finalização de operação existente
     */
    public static OperationSourceMapping forExistingOperationExit(Operation operation, InvoiceItem invoiceItem,
                                                                 Integer sequence) {
        return OperationSourceMapping.builder()
                .operation(operation)
                .invoice(invoiceItem.getInvoice())
                .invoiceItem(invoiceItem)
                .mappingType(OperationMappingType.EXISTING_OPERATION_EXIT)
                .processingSequence(sequence)
                .notes("Operação finalizada usando invoice item")
                .build();
    }

    /**
     * Cria um mapeamento para Day Trade
     */
    public static OperationSourceMapping forDayTrade(Operation operation, InvoiceItem invoiceItem,
                                                    OperationMappingType type, Integer sequence) {
        return OperationSourceMapping.builder()
                .operation(operation)
                .invoice(invoiceItem.getInvoice())
                .invoiceItem(invoiceItem)
                .mappingType(type)
                .processingSequence(sequence)
                .notes("Operação Day Trade: " + type.getDescription())
                .build();
    }
}