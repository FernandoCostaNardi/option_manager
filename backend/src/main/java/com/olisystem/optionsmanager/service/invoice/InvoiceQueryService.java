package com.olisystem.optionsmanager.service.invoice;

import com.olisystem.optionsmanager.dto.invoice.InvoiceData;
import com.olisystem.optionsmanager.dto.invoice.InvoiceFilterRequest;
import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.repository.InvoiceRepository;
import com.olisystem.optionsmanager.service.invoice.mapper.InvoiceMapperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço para consulta e listagem de notas de corretagem importadas
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class InvoiceQueryService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapperService invoiceMapperService;

    /**
     * Lista notas com filtros e paginação
     */
    public Page<InvoiceData> findInvoicesWithFilters(InvoiceFilterRequest filters, User user) {
        log.info("Consultando notas para usuário {} com filtros: {}", user.getUsername(), filters);

        PageRequest pageRequest = PageRequest.of(
            filters.page(),
            filters.size(),
            Sort.by(Sort.Direction.fromString(filters.sortDirection()), filters.sortBy())
        );

        Page<Invoice> invoicesPage;

        if (!filters.hasFilters()) {
            // Sem filtros - busca todas as notas do usuário
            invoicesPage = invoiceRepository.findByUser(user.getId(), pageRequest);
        } else {
            // Com filtros específicos
            invoicesPage = findWithSpecificFilters(filters, user, pageRequest);
        }

        // Converte para DTOs
        List<InvoiceData> invoiceDataList = invoiceMapperService.toInvoiceDataList(invoicesPage.getContent());

        return new PageImpl<>(invoiceDataList, pageRequest, invoicesPage.getTotalElements());
    }

    /**
     * Busca nota específica por ID
     */
    public Optional<InvoiceData> findInvoiceById(UUID invoiceId, User user) {
        log.debug("Buscando nota {} para usuário {}", invoiceId, user.getUsername());

        return invoiceRepository.findById(invoiceId)
            .filter(invoice -> invoice.getUser().getId().equals(user.getId()))
            .map(invoiceMapperService::toInvoiceData);
    }

    /**
     * Busca notas por corretora
     */
    public Page<InvoiceData> findInvoicesByBrokerage(UUID brokerageId, User user, int page, int size) {
        log.debug("Buscando notas da corretora {} para usuário {}", brokerageId, user.getUsername());

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "tradingDate"));
        
        Page<Invoice> invoicesPage = invoiceRepository.findByBrokerageAndUser(brokerageId, user.getId(), pageRequest);
        
        List<InvoiceData> invoiceDataList = invoiceMapperService.toInvoiceDataList(invoicesPage.getContent());

        return new PageImpl<>(invoiceDataList, pageRequest, invoicesPage.getTotalElements());
    }

    /**
     * Busca notas por período de negociação
     */
    public Page<InvoiceData> findInvoicesByDateRange(LocalDate startDate, LocalDate endDate, 
                                                    User user, int page, int size) {
        log.debug("Buscando notas entre {} e {} para usuário {}", startDate, endDate, user.getUsername());

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "tradingDate"));
        
        Page<Invoice> invoicesPage = invoiceRepository.findByUserAndDateRange(
            user.getId(), startDate, endDate, pageRequest);
        
        List<InvoiceData> invoiceDataList = invoiceMapperService.toInvoiceDataList(invoicesPage.getContent());

        return new PageImpl<>(invoiceDataList, pageRequest, invoicesPage.getTotalElements());
    }

    /**
     * Busca últimas notas importadas
     */
    public List<InvoiceData> findLatestImportedInvoices(User user, int limit) {
        log.debug("Buscando últimas {} notas importadas para usuário {}", limit, user.getUsername());

        PageRequest pageRequest = PageRequest.of(0, limit);
        List<Invoice> invoices = invoiceRepository.findLatestImportedByUser(user.getId(), pageRequest);
        
        return invoiceMapperService.toInvoiceDataList(invoices);
    }

    /**
     * Busca notas importadas em uma data específica
     */
    public List<InvoiceData> findInvoicesImportedOn(LocalDate importDate, User user) {
        log.debug("Buscando notas importadas em {} para usuário {}", importDate, user.getUsername());

        List<Invoice> invoices = invoiceRepository.findByUserAndImportDate(user.getId(), importDate);
        
        return invoiceMapperService.toInvoiceDataList(invoices);
    }

    /**
     * Conta total de notas do usuário
     */
    public Long countInvoicesByUser(User user) {
        return invoiceRepository.countByUser(user.getId());
    }

    /**
     * Conta notas por corretora
     */
    public Long countInvoicesByBrokerage(UUID brokerageId, User user) {
        return invoiceRepository.countByBrokerageAndUser(brokerageId, user.getId());
    }

    /**
     * Aplica filtros específicos na consulta
     */
    private Page<Invoice> findWithSpecificFilters(InvoiceFilterRequest filters, User user, PageRequest pageRequest) {
        
        // Se tem filtro de corretora
        if (filters.brokerageId() != null) {
            return invoiceRepository.findByBrokerageAndUser(filters.brokerageId(), user.getId(), pageRequest);
        }
        
        // Se tem filtro de período
        if (filters.hasDateRange()) {
            LocalDate startDate = filters.startDate() != null ? filters.startDate() : LocalDate.of(1990, 1, 1);
            LocalDate endDate = filters.endDate() != null ? filters.endDate() : LocalDate.now();
            
            return invoiceRepository.findByUserAndDateRange(user.getId(), startDate, endDate, pageRequest);
        }
        
        // Sem filtros específicos implementados ainda - retorna todas
        return invoiceRepository.findByUser(user.getId(), pageRequest);
    }
}
