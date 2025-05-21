package com.olisystem.optionsmanager.record.operation;

import com.olisystem.optionsmanager.model.operation.AverageOperationGroup;
import com.olisystem.optionsmanager.model.position.EntryLot;
import com.olisystem.optionsmanager.model.position.Position;
import com.olisystem.optionsmanager.model.transaction.TransactionType;

import java.util.List;

public record OperationExitPositionContext(OperationExitContext context, 
                                           AverageOperationGroup group,
                                           TransactionType transactionType,
                                           Position position,
                                           List<EntryLot> availableLots)
{}
