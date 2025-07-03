package com.olisystem.optionsmanager.config.invoice;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configurações específicas para o sistema de importação de notas de corretagem
 */
@Configuration
@ConfigurationProperties(prefix = "app.invoice")
@Data
public class InvoiceConfigurationProperties {

    /**
     * Configurações de importação
     */
    private ImportConfig importConfig = new ImportConfig();

    /**
     * Configurações de arquivo
     */
    private FileConfig fileConfig = new FileConfig();

    /**
     * Configurações de parser
     */
    private ParserConfig parserConfig = new ParserConfig();

    @Data
    public static class ImportConfig {
        /**
         * Máximo de arquivos por importação
         */
        private int maxFilesPerImport = 5;

        /**
         * Timeout para processamento (em segundos)
         */
        private int processingTimeoutSeconds = 300;

        /**
         * Se deve processar importação em paralelo
         */
        private boolean parallelProcessing = false;

        /**
         * Se deve validar hashes duplicados automaticamente
         */
        private boolean validateDuplicates = true;
    }

    @Data
    public static class FileConfig {
        /**
         * Tamanho máximo de arquivo (em bytes)
         */
        private long maxFileSizeBytes = 10 * 1024 * 1024; // 10MB

        /**
         * Tamanho mínimo de conteúdo (em caracteres)
         */
        private int minContentLength = 1000;

        /**
         * Extensões de arquivo permitidas
         */
        private String[] allowedExtensions = {"pdf"};

        /**
         * Se deve compactar conteúdo antes de salvar
         */
        private boolean compressContent = false;
    }

    @Data
    public static class ParserConfig {
        /**
         * Parser padrão a ser usado
         */
        private String defaultParser = "basic";

        /**
         * Se deve usar parser específico por corretora quando disponível
         */
        private boolean useBrokerageSpecificParsers = true;

        /**
         * Timeout para parsing (em segundos)
         */
        private int parsingTimeoutSeconds = 60;

        /**
         * Se deve extrair dados financeiros detalhados
         */
        private boolean extractDetailedFinancials = true;

        /**
         * Se deve extrair observações dos itens
         */
        private boolean extractItemObservations = true;
    }
}
