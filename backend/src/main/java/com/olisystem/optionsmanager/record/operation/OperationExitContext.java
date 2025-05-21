package com.olisystem.optionsmanager.record.operation;

import com.olisystem.optionsmanager.dto.operation.OperationFinalizationRequest;
import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.operation.Operation;

public record OperationExitContext(OperationFinalizationRequest request,
                                   Operation activeOperation,
                                   User currentUser
) {}
