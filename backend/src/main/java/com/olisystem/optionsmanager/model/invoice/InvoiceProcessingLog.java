package com.olisystem.optionsmanager.model.invoice;

import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.enums.InvoiceProcessingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entity para log de processamento de invoices
 * Rastreia o processamento de cada invoice para criação de operações
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-03
 */
@Entity
@Table(name = "invoice_processing_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceProcessingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private InvoiceProcessingStatus status = InvoiceProcessingStatus.PENDING;

    @Column(name = "operations_created", nullable = false)
    @Builder.Default
    private Integer operationsCreated = 0;

    @Column(name = "operations_updated", nullable = false)
    @Builder.Default
    private Integer operationsUpdated = 0;

    @Column(name = "operations_skipped", nullable = false)
    @Builder.Default
    private Integer operationsSkipped = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "processing_details", columnDefinition = "jsonb")
    private Map<String, Object> processingDetails;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "processing_duration_ms")
    private Long processingDurationMs;

    @Column(name = "reprocessed_count", nullable = false)
    @Builder.Default
    private Integer reprocessedCount = 0;

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
     * Calcula a duração do processamento se startedAt e completedAt estão definidos
     */
    public void calculateProcessingDuration() {
        if (startedAt != null && completedAt != null) {
            this.processingDurationMs = java.time.Duration.between(startedAt, completedAt).toMillis();
        }
    }

    /**
     * Marca o início do processamento
     */
    public void markAsStarted() {
        this.startedAt = LocalDateTime.now();
        this.status = InvoiceProcessingStatus.PROCESSING;
    }

    /**
     * Marca a conclusão do processamento
     */
    public void markAsCompleted(InvoiceProcessingStatus finalStatus) {
        this.completedAt = LocalDateTime.now();
        this.status = finalStatus;
        calculateProcessingDuration();
    }
}