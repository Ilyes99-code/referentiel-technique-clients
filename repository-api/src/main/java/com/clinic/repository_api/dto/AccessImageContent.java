package com.clinic.repository_api.dto;

/**
 * The bytes of one image plus the two headers needed to serve them. Exists so the
 * service can hand the controller a payload without leaking the AccessImage entity
 * (and its Client association) past the service boundary.
 */
public record AccessImageContent(
        String fileName,
        String contentType,
        byte[] data
) {}
