package com.olisystem.optionsmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.olisystem.optionsmanager.model.invoice.InvoiceItem;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {}
