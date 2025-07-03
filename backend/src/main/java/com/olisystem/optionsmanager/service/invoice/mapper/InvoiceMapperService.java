package com.olisystem.optionsmanager.service.invoice.mapper;

import com.olisystem.optionsmanager.dto.invoice.InvoiceData;
import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço para mapeamento entre entidades e DTOs de Invoice
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceMapperService {

    /**
     * Converte Invoice entidade para InvoiceData DTO
     */
    public InvoiceData toInvoiceData(Invoice invoice) {
        if (invoice == null) {
            return null;
        }

        // Mapeia itens
        List<InvoiceData.InvoiceItemData> itemsData = invoice.getItems() != null 
            ? invoice.getItems().stream()
                .map(this::toInvoiceItemData)
                .collect(Collectors.toList())
            : List.of();

        return new InvoiceData(
            invoice.getId(),
            invoice.getInvoiceNumber(),
            invoice.getTradingDate(),
            invoice.getSettlementDate(),
            invoice.getClientName(),
            invoice.getBrokerage() != null ? invoice.getBrokerage().getName() : "N/A",
            invoice.getGrossOperationsValue(),
            invoice.getNetOperationsValue(),
            invoice.getTotalCosts(),
            invoice.getNetSettlementValue(),
            invoice.getItems() != null ? invoice.getItems().size() : 0,
            invoice.getImportedAt(),
            itemsData
        );
    }
    /**
     * Converte InvoiceItem entidade para InvoiceItemData DTO
     */
    public InvoiceData.InvoiceItemData toInvoiceItemData(InvoiceItem item) {
        if (item == null) {
            return null;
        }

        return new InvoiceData.InvoiceItemData(
            item.getId(),
            item.getSequenceNumber(),
            item.getOperationType(),
            item.getMarketType(),
            item.getAssetCode(),
            item.getAssetSpecification(),
            item.getQuantity(),
            item.getUnitPrice(),
            item.getTotalValue(),
            item.getIsDayTrade(),
            item.getExpirationDate(),
            item.getStrikePrice(),
            item.getObservations()
        );
    }

    /**
     * Converte lista de Invoices para lista de InvoiceData
     */
    public List<InvoiceData> toInvoiceDataList(List<Invoice> invoices) {
        if (invoices == null) {
            return List.of();
        }

        return invoices.stream()
            .map(this::toInvoiceData)
            .collect(Collectors.toList());
    }
}