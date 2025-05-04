package com.olisystem.optionsmanager.service.option_series;

import com.olisystem.optionsmanager.client.OptionInfoClient;
import com.olisystem.optionsmanager.dto.option.OptionInfoResponseDto;
import com.olisystem.optionsmanager.model.option_serie.OptionType;
import com.olisystem.optionsmanager.parser.OptionInfoHtmlParser;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OptionInfoService {
  private final OptionInfoClient client;
  private final OptionInfoHtmlParser parser;

  public OptionInfoService() {
    this.client = new OptionInfoClient();
    this.parser = new OptionInfoHtmlParser();
  }

  /**
   * Obtém informações de strike, data de vencimento, tipo da opção e ativo base a partir do site
   * opcoes.net.br
   *
   * @param optionCode Código da opção (ex: MRFGQ214)
   * @return Um objeto com o strike, data de vencimento, tipo da opção e ativo base
   * @throws Exception Em caso de erro na comunicação ou na extração de dados
   */
  public OptionInfoResponseDto getOptionInfo(String optionCode) throws Exception {
    try {
      String html = client.fetchOptionHtml(optionCode);

      BigDecimal strike = parser.extractStrike(html);
      LocalDate expiryDate = parser.extractExpiryDate(html);
      OptionType optionType = parser.extractOptionType(html).orElse(null);
      String baseAsset = parser.extractBaseAsset(html).orElse("");

      log.info("strike: {}", strike);
      log.info("data de vencimento: {}", expiryDate);
      log.info("tipo da opção: {}", optionType);
      log.info("ativo base: {}", baseAsset);

      return new OptionInfoResponseDto(strike, expiryDate, optionType, baseAsset);
    } catch (Exception e) {
      throw new Exception("Erro ao buscar informações da opção: " + e.getMessage(), e);
    }
  }
}
