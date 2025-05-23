package com.olisystem.optionsmanager.controller.option;

import com.olisystem.optionsmanager.dto.option.OptionDataResponseDto;
import com.olisystem.optionsmanager.service.option_series.OptionSerieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/option-series")
public class OptionSerieController {

  @Autowired private OptionSerieService optionSerieService;

  @GetMapping("/{code}")
  public ResponseEntity<OptionDataResponseDto> buscarOpcaoInfo(@PathVariable String code)
      throws Exception {
    OptionDataResponseDto optionDataResponseDto = optionSerieService.buscarOpcaoInfo(code);
    return new ResponseEntity<>(optionDataResponseDto, HttpStatus.OK);
  }
}
