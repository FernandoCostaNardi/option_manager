package com.olisystem.optionsmanager.service.option_series;

import com.olisystem.optionsmanager.client.BrapiClient;
import com.olisystem.optionsmanager.dto.option.OptionDataResponseDto;
import com.olisystem.optionsmanager.dto.option.OptionInfoResponseDto;
import com.olisystem.optionsmanager.model.Asset.Asset;
import com.olisystem.optionsmanager.model.Asset.AssetType;
import com.olisystem.optionsmanager.model.option_serie.OptionSerie;
import com.olisystem.optionsmanager.parser.AssetFactory;
import com.olisystem.optionsmanager.repository.OptionSerieRepository;
import com.olisystem.optionsmanager.util.UuidUtil;
import java.util.List;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class OptionSerieService {

  private static final String BRAPI_TOKEN = "kyKjkNQAf8QC5gHedBRhqm";

  @Autowired private OptionSerieRepository optionSerieRepository;

  @Autowired private OptionInfoService optionInfoService;

  public List<OptionSerie> findAll() {
    return optionSerieRepository.findAll();
  }

  public Optional<OptionSerie> findById(String id) {
    return optionSerieRepository.findById(UuidUtil.parseUuid(id));
  }

  public OptionSerie getOptionSerieByCode(String code) {
    return optionSerieRepository.findByCode(code).orElse(null);
  }

  public OptionSerie save(OptionSerie optionSerie) {
    return optionSerieRepository.save(optionSerie);
  }

  public void deleteById(String id) {
    optionSerieRepository.deleteById(UuidUtil.parseUuid(id));
  }

  public OptionDataResponseDto buscarOpcaoInfo(String codigoOpcao) throws Exception {
    if (codigoOpcao == null || codigoOpcao.length() < 7) {
      throw new IllegalArgumentException("Código da opção inválido: " + codigoOpcao);
    }

    OptionInfoResponseDto optionData = optionInfoService.getOptionInfo(codigoOpcao);
    log.info("Recebido de getOpcaoInfo: " + optionData);
    Asset ativoInfo = buscarAtivoInfo(optionData.getBaseAsset());

    OptionDataResponseDto info = new OptionDataResponseDto();
    info.setOptionCode(codigoOpcao);
    info.setBaseAsset(optionData.getBaseAsset());
    info.setBaseAssetName(ativoInfo.getName());
    info.setBaseAssetUrlLogo(ativoInfo.getUrlLogo());
    info.setBaseAssetType(AssetType.STOCK);
    info.setBaseAsset(optionData.getBaseAsset());
    info.setOptionStrikePrice(optionData.getStrikePrice());
    info.setOptionExpirationDate(optionData.getExpirationDate());
    info.setOptionType(optionData.getOptionType());

    return info;
  }

  private Asset buscarAtivoInfo(String ticker) throws Exception {
    BrapiClient brapiClient = new BrapiClient(BRAPI_TOKEN);
    JSONObject json = brapiClient.getQuoteJson(ticker);
    return AssetFactory.fromJson(json);
  }
}
