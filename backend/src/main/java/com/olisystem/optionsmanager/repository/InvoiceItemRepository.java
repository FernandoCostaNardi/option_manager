package com.olisystem.optionsmanager.repository;

import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {}
