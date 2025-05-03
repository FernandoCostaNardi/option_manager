package com.olisystem.optionsmanager.util;

import com.olisystem.optionsmanager.exception.InvalidIdFormatException;
import java.util.UUID;

public class UuidUtil {
    
    public static UUID parseUuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new InvalidIdFormatException("Formato de UUID inválido: " + id);
        }
    }
} 