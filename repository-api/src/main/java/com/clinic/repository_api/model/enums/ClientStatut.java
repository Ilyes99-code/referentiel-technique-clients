package com.clinic.repository_api.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ClientStatut {
    EN_REGLE("en regle"),
    SUSPENDU("suspendu");

    private final String jsonValue;

    ClientStatut(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonValue
    public String getJsonValue() {
        return jsonValue;
    }

    @JsonCreator
    public static ClientStatut fromJson(String value) {
        if (value == null) {
            return null;
        }
        return switch (value.trim().toLowerCase()) {
            case "en regle", "en_regle" -> EN_REGLE;
            case "suspendu" -> SUSPENDU;
            default -> throw new IllegalArgumentException(
                    "Statut client invalide: " + value + ". Valeurs autorisées: en regle, suspendu");
        };
    }
}
