package com.olisystem.optionsmanager.repository;

import com.olisystem.optionsmanager.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {}
