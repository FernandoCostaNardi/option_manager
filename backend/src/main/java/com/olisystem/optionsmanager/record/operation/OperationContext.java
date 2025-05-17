package com.olisystem.optionsmanager.record.operation;

import com.olisystem.optionsmanager.dto.operation.OperationDataRequest;
import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.option_serie.OptionSerie;

public record OperationContext(OperationDataRequest request,
                               OptionSerie optionSerie,
                               User currentUser
) {}
