package com.clinic.repository_api.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.clinic.repository_api.dto.AccessImageContent;
import com.clinic.repository_api.dto.AccessImageDto;
import com.clinic.repository_api.model.AccessImage;
import com.clinic.repository_api.model.Client;
import com.clinic.repository_api.repository.AccessImageRepository;
import com.clinic.repository_api.repository.AccessImageRepository.AccessImageSummary;
import com.clinic.repository_api.repository.ClientRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class AccessImageService {

    /**
     * Defence in depth against a client filling the database. The multipart limits in
     * application.properties bound a single request; this bounds the total.
     */
    private static final int MAX_IMAGES_PER_CLIENT = 40;

    private static final String PNG = "image/png";
    private static final String JPEG = "image/jpeg";
    private static final String GIF = "image/gif";
    private static final String WEBP = "image/webp";

    private final AccessImageRepository imageRepository;
    private final ClientRepository clientRepository;

    public AccessImageService(AccessImageRepository imageRepository, ClientRepository clientRepository) {
        this.imageRepository = imageRepository;
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public List<AccessImageDto> findAllByClient(Long clientId) {
        requireClientExists(clientId);
        return imageRepository.findSummaryByClientIdOrderByUploadedAtDescIdDesc(clientId).stream()
                .map(summary -> toDto(clientId, summary))
                .toList();
    }

    @Transactional(readOnly = true)
    public AccessImageContent loadContent(Long clientId, Long imageId) {
        AccessImage image = imageRepository.findByIdAndClientId(imageId, clientId)
                .orElseThrow(() -> new EntityNotFoundException("Image introuvable: " + imageId));
        return new AccessImageContent(image.getFileName(), image.getContentType(), image.getData());
    }

    @Transactional
    public AccessImageDto upload(Long clientId, MultipartFile file) {
        Client client = requireClient(clientId);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Aucun fichier reçu.");
        }
        if (file.getSize() > AccessImage.MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException(
                    "Fichier trop volumineux — 5 Mo maximum par image.");
        }
        if (imageRepository.countByClientId(clientId) >= MAX_IMAGES_PER_CLIENT) {
            throw new IllegalArgumentException(
                    "Limite atteinte — " + MAX_IMAGES_PER_CLIENT + " images maximum par client.");
        }

        byte[] bytes = readBytes(file);

        // Re-checked against the materialised array: getSize() reports what the client
        // declared in the multipart part, which is not authoritative.
        if (bytes.length > AccessImage.MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Fichier trop volumineux — 5 Mo maximum par image.");
        }

        // The stored type comes from the file's own signature, never from the part's
        // declared Content-Type. Trusting the declared value would let an attacker
        // store, say, an SVG (which can carry <script>) or an HTML document under
        // image/png and have this API serve it back with a Content-Type the browser
        // will happily execute. SVG is intentionally absent from the whitelist below
        // for exactly that reason.
        String contentType = detectContentType(bytes)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Format non pris en charge — utilisez PNG, JPG, WEBP ou GIF."));

        AccessImage image = new AccessImage();
        image.setClient(client);
        image.setFileName(sanitizeFileName(file.getOriginalFilename(), contentType));
        image.setContentType(contentType);
        image.setSizeBytes(bytes.length);
        image.setData(bytes);
        image.setUploadedAt(Instant.now());

        AccessImage saved = imageRepository.save(image);
        return new AccessImageDto(
                saved.getId(),
                clientId,
                saved.getFileName(),
                saved.getContentType(),
                saved.getSizeBytes(),
                saved.getUploadedAt());
    }

    @Transactional
    public void delete(Long clientId, Long imageId) {
        // A row count of 0 means either "no such image" or "belongs to another client".
        // Both are reported as 404 on purpose: distinguishing them would confirm the
        // existence of images the caller is not scoped to.
        if (imageRepository.deleteByIdAndClientId(imageId, clientId) == 0) {
            throw new EntityNotFoundException("Image introuvable: " + imageId);
        }
    }

    private Client requireClient(Long clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Client introuvable: " + clientId));
    }

    private void requireClientExists(Long clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw new EntityNotFoundException("Client introuvable: " + clientId);
        }
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new UncheckedIOException("Lecture du fichier impossible", ex);
        }
    }

    /**
     * Identifies the image format from its leading bytes (its "magic number").
     * Returns empty for anything that is not one of the four supported raster formats.
     */
    private static Optional<String> detectContentType(byte[] b) {
        if (startsWith(b, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
            return Optional.of(PNG);
        }
        if (startsWith(b, 0xFF, 0xD8, 0xFF)) {
            return Optional.of(JPEG);
        }
        // "GIF87a" / "GIF89a"
        if (startsWith(b, 'G', 'I', 'F', '8') && b.length > 5
                && (b[4] == '7' || b[4] == '9') && b[5] == 'a') {
            return Optional.of(GIF);
        }
        // RIFF container whose form type is WEBP: "RIFF" <4-byte size> "WEBP"
        if (startsWith(b, 'R', 'I', 'F', 'F') && b.length >= 12
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return Optional.of(WEBP);
        }
        return Optional.empty();
    }

    private static boolean startsWith(byte[] bytes, int... signature) {
        if (bytes.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((bytes[i] & 0xFF) != (signature[i] & 0xFF)) {
                return false;
            }
        }
        return true;
    }

    /**
     * The original filename is attacker-controlled text. It is never used to touch the
     * filesystem here, but it is echoed back in Content-Disposition and rendered in the
     * UI, so path segments, control characters and unbounded length are all stripped.
     */
    private static String sanitizeFileName(String original, String contentType) {
        if (original == null) {
            return defaultFileName(contentType);
        }

        // Keep only the last path segment — browsers normally send a bare name, but a
        // crafted request can send "../../etc/passwd" or a Windows path.
        String name = original.replace('\\', '/');
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash >= 0) {
            name = name.substring(lastSlash + 1);
        }

        name = name.replaceAll("[\\p{Cntrl}]", "").trim();
        if (name.isEmpty() || ".".equals(name) || "..".equals(name)) {
            return defaultFileName(contentType);
        }
        return name.length() > 255 ? name.substring(0, 255) : name;
    }

    private static String defaultFileName(String contentType) {
        String extension = switch (contentType) {
            case PNG -> "png";
            case JPEG -> "jpg";
            case GIF -> "gif";
            case WEBP -> "webp";
            default -> "img";
        };
        return "capture." + extension;
    }

    private static AccessImageDto toDto(Long clientId, AccessImageSummary summary) {
        return new AccessImageDto(
                summary.getId(),
                clientId,
                summary.getFileName(),
                summary.getContentType(),
                summary.getSizeBytes(),
                summary.getUploadedAt());
    }
}
