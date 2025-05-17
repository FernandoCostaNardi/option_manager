package com.olisystem.optionsmanager.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.olisystem.optionsmanager.repository")
@EntityScan(basePackages = "com.olisystem.optionsmanager.model")
public class RepositoryConfig {}
