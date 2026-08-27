package com.clinic.repository_api.model;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A screenshot / visual attached to a client's technical-access section (network
 * diagram, VPN client configuration, firewall rule capture...).
 *
 * The bytes live in the database rather than on disk: the deployment is
 * containerised, so a filesystem path would need a mounted volume and a separate
 * backup story, and deleting a client would leave orphaned files behind. A BYTEA
 * column cascades with the client row and is covered by the normal DB dump.
 *
 * The trade-off is that the payload must never be selected unless it is actually
 * being streamed — see AccessImageRepository.AccessImageSummary, which is what the
 * listing endpoint uses.
 */
@Entity
@Table(name = "acces_technique_images")
public class AccessImage {

    /**
     * Upper bound on a single stored image, in bytes. Mirrored by
     * spring.servlet.multipart.max-file-size (which rejects the upload before it is
     * fully buffered) and by AccessImageService's own check (which is what actually
     * guarantees the invariant, since the multipart limit is configuration).
     */
    public static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /**
     * Always server-determined from the file's magic bytes, never copied from the
     * multipart part's declared Content-Type — see AccessImageService.detectContentType.
     * This is what makes it safe to echo back as the response Content-Type when the
     * image is streamed.
     */
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    /**
     * Deliberately NOT annotated @Lob: on PostgreSQL, Hibernate maps a @Lob byte[] to
     * an `oid` large object (out-of-line storage, requires the LO API and a live
     * transaction), whereas VARBINARY maps to `bytea` — which is what
     * V5__add_access_images.sql creates and what ddl-auto=validate expects in prod.
     *
     * The JDBC type is pinned rather than inferred because inference is
     * dialect-dependent: a length above the dialect's max varbinary size makes
     * Hibernate silently promote the type to BLOB (H2 generated `data blob` from this
     * exact mapping before the annotation was added), and BLOB on PostgreSQL is `oid`
     * again. Pinning VARBINARY yields `varbinary(5242880)` on H2 and `bytea` on
     * PostgreSQL, which is what both profiles need. The length itself only matters to
     * H2 + ddl-auto=update, where the default would be VARBINARY(255).
     */
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "data", nullable = false, length = MAX_IMAGE_BYTES)
    private byte[] data;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    public AccessImage() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
