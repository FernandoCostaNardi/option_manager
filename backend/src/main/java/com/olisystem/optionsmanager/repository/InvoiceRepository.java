package com.olisystem.optionsmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.olisystem.optionsmanager.model.invoice.Invoice;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {}
