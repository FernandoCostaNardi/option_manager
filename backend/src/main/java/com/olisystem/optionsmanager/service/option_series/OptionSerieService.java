package com.olisystem.optionsmanager.service.option_series;

import java.util.List;
import java.util.Optional;

import com.olisystem.optionsmanager.dto.operation.OperationDataRequest;
import com.olisystem.optionsmanager.dto.option.OptionDataResponseDto;
import com.olisystem.optionsmanager.model.Asset.Asset;
import com.olisystem.optionsmanager.model.option_serie.OptionSerie;

public interface OptionSerieService {

  List<OptionSerie> findAll();

  Optional<OptionSerie> findById(String id);

  OptionSerie getOptionSerieByCode(String code);

  OptionSerie save(OptionSerie optionSerie);

  void deleteById(String id);

  OptionDataResponseDto buscarOpcaoInfo(String codigoOpcao) throws Exception;

  Asset buscarAtivoInfo(String ticker) throws Exception;

  OptionSerie findOrCreateOptionSerie(OperationDataRequest request, Asset asset);

}
