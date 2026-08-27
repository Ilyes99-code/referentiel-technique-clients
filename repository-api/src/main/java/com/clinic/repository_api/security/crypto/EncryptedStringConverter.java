package com.clinic.repository_api.security.crypto;

import org.springframework.stereotype.Component;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA converter that transparently encrypts a field on write and decrypts it on
 * read. Registered as a Spring bean (autoApply = false, applied explicitly via
 * @Convert) so Spring Boot's Hibernate bean-container integration injects
 * CredentialCryptoService instead of Hibernate instantiating it with `new`.
 */
@Converter(autoApply = false)
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final CredentialCryptoService cryptoService;

    public EncryptedStringConverter(CredentialCryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return cryptoService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return cryptoService.decrypt(dbData);
    }
}
