package com.olisystem.optionsmanager.parser;

import com.olisystem.optionsmanager.model.option_serie.OptionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OptionInfoHtmlParser {
  private static final Pattern TITLE_PATTERN = Pattern.compile("<title>(.*?)</title>");
  private static final Pattern STRIKE_PATTERN = Pattern.compile("Strike R\\$ (\\d+[,.]\\d+)");
  private static final Pattern EXPIRY_DATE_PATTERN =
      Pattern.compile("Vencimento (\\d{2}/\\d{2}/\\d{4})");
  private static final Pattern META_PATTERN =
      Pattern.compile("<meta name=\"description\" content=\"(.*?)\">");
  private static final Pattern OPTION_DETAILS_PATTERN =
      Pattern.compile("- (CALL|PUT) de ([A-Z0-9]+)");
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  /** Extrai o valor do strike do HTML */
  public BigDecimal extractStrike(String html) throws Exception {
    String titleContent = extractTitleContent(html);
    log.info("Conteúdo do título: {}", titleContent);

    Matcher strikeMatcher = STRIKE_PATTERN.matcher(titleContent);
    if (!strikeMatcher.find()) {
      throw new Exception("Não foi possível extrair o valor do strike do título");
    }

    String strikeStr = strikeMatcher.group(1).replace(',', '.');
    return BigDecimal.valueOf(Double.parseDouble(strikeStr));
  }

  /** Extrai a data de vencimento do HTML */
  public LocalDate extractExpiryDate(String html) throws Exception {
    String titleContent = extractTitleContent(html);

    Matcher dataMatcher = EXPIRY_DATE_PATTERN.matcher(titleContent);
    if (!dataMatcher.find()) {
      throw new Exception("Não foi possível extrair a data de vencimento do título");
    }

    String dataStr = dataMatcher.group(1);
    return LocalDate.parse(dataStr, DATE_FORMATTER);
  }

  /** Extrai o tipo da opção (CALL/PUT) do HTML */
  public Optional<OptionType> extractOptionType(String html) {
    Optional<String> metaContent = extractMetaContent(html);

    return metaContent.flatMap(
        content -> {
          Matcher matcher = OPTION_DETAILS_PATTERN.matcher(content);
          if (matcher.find()) {
            try {
              return Optional.of(OptionType.valueOf(matcher.group(1)));
            } catch (IllegalArgumentException e) {
              log.warn("Tipo de opção inválido: {}", matcher.group(1));
              return Optional.empty();
            }
          }
          return Optional.empty();
        });
  }

  /** Extrai o ativo base do HTML */
  public Optional<String> extractBaseAsset(String html) {
    Optional<String> metaContent = extractMetaContent(html);

    return metaContent.flatMap(
        content -> {
          Matcher matcher = OPTION_DETAILS_PATTERN.matcher(content);
          if (matcher.find()) {
            return Optional.of(matcher.group(2));
          }
          return Optional.empty();
        });
  }

  private String extractTitleContent(String html) throws Exception {
    Matcher titleMatcher = TITLE_PATTERN.matcher(html);
    if (!titleMatcher.find()) {
      throw new Exception("Não foi possível encontrar o título da página");
    }
    return titleMatcher.group(1);
  }

  private Optional<String> extractMetaContent(String html) {
    Matcher metaMatcher = META_PATTERN.matcher(html);
    if (metaMatcher.find()) {
      String metaContent = metaMatcher.group(1);
      log.info("Conteúdo da meta descrição: {}", metaContent);
      return Optional.of(metaContent);
    } else {
      log.warn("Não foi possível encontrar a meta descrição");
      return Optional.empty();
    }
  }
}
