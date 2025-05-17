package com.olisystem.optionsmanager.service.operation.consolidate;

import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.position.Position;

public interface OperationConsolidateService {

    public Operation consolidateOperationEntryValues(Operation consolidatedEntry, Position position);

    

}
