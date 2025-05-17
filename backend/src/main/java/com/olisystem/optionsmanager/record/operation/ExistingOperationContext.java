package com.olisystem.optionsmanager.record.operation;

import com.olisystem.optionsmanager.dto.operation.OperationDataRequest;
import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.operation.AverageOperationGroup;
import com.olisystem.optionsmanager.model.operation.AverageOperationItem;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.option_serie.OptionSerie;
import com.olisystem.optionsmanager.model.position.Position;

public record ExistingOperationContext(
        OperationDataRequest request,
        OptionSerie optionSerie,
        User currentUser,
        Operation activeOperation,
        AverageOperationGroup group,
        AverageOperationItem itemGroup,
        Position position
) {}
