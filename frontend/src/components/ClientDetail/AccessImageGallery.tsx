import React, { useCallback, useEffect, useRef, useState } from "react";
import {
  ApiError,
  deleteAccessImage,
  fetchAccessImageBlob,
  getAccessImages,
  uploadAccessImage,
} from "../../services/api";
import type { AccessImageDto } from "../../types/models";

const MAX_FILE_BYTES = 5 * 1024 * 1024;

// Client-side gate only — fast feedback so an obviously wrong file never leaves the
// browser. It is NOT the security boundary: File.type comes from the OS by extension
// and is trivially spoofed, so the server re-derives the real format from the file's
// magic bytes and ignores whatever we send.
const ACCEPTED_TYPES = ["image/png", "image/jpeg", "image/webp", "image/gif"];
const ACCEPT_ATTRIBUTE = ACCEPTED_TYPES.join(",");

const formatBytes = (bytes: number): string => {
  if (bytes < 1024) return `${bytes} o`;
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} Ko`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`;
};

const formatDate = (iso: string): string => {
  const parsed = new Date(iso);
  if (Number.isNaN(parsed.getTime())) return "";
  return parsed.toLocaleDateString("fr-FR", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  });
};

const rejectionReason = (file: File): string | null => {
  if (file.size > MAX_FILE_BYTES) {
    return `« ${file.name} » dépasse 5 Mo (${formatBytes(file.size)}).`;
  }
  if (file.type && !ACCEPTED_TYPES.includes(file.type)) {
    return `« ${file.name} » n'est pas une image prise en charge (PNG, JPG, WEBP ou GIF).`;
  }
  return null;
};

interface AccessImageGalleryProps {
  clientId: number;
  token: string;
  onNotify: (message: string) => void;
}

export const AccessImageGallery: React.FC<AccessImageGalleryProps> = ({
  clientId,
  token,
  onNotify,
}) => {
  const [images, setImages] = useState<AccessImageDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState<{ done: number; total: number } | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [dragActive, setDragActive] = useState(false);
  const [lightboxIndex, setLightboxIndex] = useState<number | null>(null);
  // Armed by the × button, committed by the overlay — a deleted image cannot be
  // recovered from the UI, so a single mis-click must not destroy one.
  const [confirmDeleteId, setConfirmDeleteId] = useState<number | null>(null);
  const [pendingDeleteId, setPendingDeleteId] = useState<number | null>(null);

  // imageId -> blob: URL. Mirrored into a ref so the unmount cleanup can revoke every
  // URL without the effect having to depend on (and therefore re-run for) the map.
  const [objectUrls, setObjectUrls] = useState<Record<number, string>>({});
  const objectUrlsRef = useRef<Record<number, string>>({});

  const fileInputRef = useRef<HTMLInputElement>(null);
  const lightboxCloseRef = useRef<HTMLButtonElement>(null);
  // Element that opened the lightbox, so focus can be handed back on close.
  const lightboxOpenerRef = useRef<HTMLElement | null>(null);
  // Guards against a drag that passes over child elements firing dragleave and
  // making the drop zone flicker — enter/leave are counted rather than toggled.
  const dragDepthRef = useRef(0);

  const setUrl = useCallback((imageId: number, url: string) => {
    objectUrlsRef.current = { ...objectUrlsRef.current, [imageId]: url };
    setObjectUrls(objectUrlsRef.current);
  }, []);

  const dropUrl = useCallback((imageId: number) => {
    const existing = objectUrlsRef.current[imageId];
    if (!existing) return;
    URL.revokeObjectURL(existing);
    const rest = { ...objectUrlsRef.current };
    delete rest[imageId];
    objectUrlsRef.current = rest;
    setObjectUrls(rest);
  }, []);

  const describeError = (err: unknown, fallback: string): string => {
    if (!(err instanceof ApiError)) return fallback;

    // 413 can come straight from the servlet container with no JSON body at all,
    // so it is answered before the fromServer check.
    if (err.status === 413) return "Fichier trop volumineux — 5 Mo maximum par image.";

    // 403/404 here means the route is not on the server. It cannot mean "unknown
    // client" — ClientDetail already loaded this client successfully before
    // rendering — and it cannot mean "not an admin", since every other call on
    // this page succeeded with the same token. In practice it is an API that
    // predates this feature, and "Request failed (403)" sent people looking in
    // entirely the wrong place.
    if (err.status === 403 || err.status === 404) {
      return "Cette API ne gère pas encore les captures. Le backend doit être redémarré avec la version qui expose /access-images.";
    }

    // Anything else: the server's own message if it actually sent one (those are
    // already written for end users), never the synthesised "Request failed (n)".
    return err.fromServer ? err.message : fallback;
  };

  useEffect(() => {
    let cancelled = false;

    getAccessImages(clientId, token)
      .then((data) => {
        if (!cancelled) setImages(data);
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(describeError(err, "Impossible de charger les images."));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [clientId, token]);

  // Resolves each image's bytes into a blob: URL. Idempotent — anything already
  // resolved is skipped, so re-running on a new `images` identity costs nothing.
  useEffect(() => {
    let cancelled = false;

    const resolve = async () => {
      for (const image of images) {
        if (cancelled) return;
        if (objectUrlsRef.current[image.id]) continue;
        try {
          const blob = await fetchAccessImageBlob(clientId, image.id, token);
          if (cancelled) return;
          setUrl(image.id, URL.createObjectURL(blob));
        } catch {
          // Leave the tile in its placeholder state — a single unreadable image
          // must not blank the whole gallery.
        }
      }
    };

    void resolve();
    return () => {
      cancelled = true;
    };
  }, [images, clientId, token, setUrl]);

  // Revoke every outstanding blob: URL on unmount. Without this the decoded bitmaps
  // stay pinned for the lifetime of the document, which on a page the user navigates
  // in and out of adds up quickly.
  useEffect(
    () => () => {
      Object.values(objectUrlsRef.current).forEach(URL.revokeObjectURL);
      objectUrlsRef.current = {};
    },
    [],
  );

  const closeLightbox = useCallback(() => {
    setLightboxIndex(null);
    lightboxOpenerRef.current?.focus();
    lightboxOpenerRef.current = null;
  }, []);

  const stepLightbox = useCallback(
    (delta: number) => {
      setLightboxIndex((current) => {
        if (current === null || images.length === 0) return current;
        return (current + delta + images.length) % images.length;
      });
    },
    [images.length],
  );

  // Keyboard control + scroll lock while the lightbox is open.
  useEffect(() => {
    if (lightboxIndex === null) return;

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        event.preventDefault();
        closeLightbox();
      } else if (event.key === "ArrowRight") {
        event.preventDefault();
        stepLightbox(1);
      } else if (event.key === "ArrowLeft") {
        event.preventDefault();
        stepLightbox(-1);
      }
    };

    window.addEventListener("keydown", onKeyDown);
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    lightboxCloseRef.current?.focus();

    return () => {
      window.removeEventListener("keydown", onKeyDown);
      document.body.style.overflow = previousOverflow;
    };
  }, [lightboxIndex, closeLightbox, stepLightbox]);

  const handleFiles = async (fileList: FileList | null) => {
    if (!fileList || fileList.length === 0) return;

    const files = Array.from(fileList);
    const rejections = files.map(rejectionReason).filter((r): r is string => r !== null);
    const accepted = files.filter((file) => rejectionReason(file) === null);

    setError(rejections.length > 0 ? rejections.join(" ") : null);
    if (accepted.length === 0) return;

    setUploading({ done: 0, total: accepted.length });
    const uploaded: AccessImageDto[] = [];

    try {
      // Sequential rather than Promise.all: the per-client cap and the multipart
      // limits are enforced server-side, and a serial loop keeps an error
      // attributable to one specific file instead of failing the whole batch.
      for (const file of accepted) {
        const created = await uploadAccessImage(clientId, file, token);
        uploaded.push(created);
        setUploading((current) =>
          current ? { ...current, done: current.done + 1 } : current,
        );
      }
      onNotify(
        uploaded.length > 1 ? `${uploaded.length} images ajoutées` : "Image ajoutée",
      );
    } catch (err) {
      const message = describeError(err, "Échec de l'envoi de l'image.");
      setError(message);
      // Also raised as a toast: the inline banner alone was missed entirely in
      // practice, because a failed upload otherwise looks identical to not having
      // clicked anything.
      onNotify(message);
    } finally {
      if (uploaded.length > 0) {
        setImages((current) => [...uploaded.reverse(), ...current]);
      }
      setUploading(null);
    }
  };

  const handleDelete = async (image: AccessImageDto) => {
    setPendingDeleteId(image.id);
    try {
      await deleteAccessImage(clientId, image.id, token);
      dropUrl(image.id);
      setImages((current) => current.filter((candidate) => candidate.id !== image.id));
      setLightboxIndex(null);
      setConfirmDeleteId(null);
      onNotify("Image supprimée");
    } catch (err) {
      setError(describeError(err, "Échec de la suppression de l'image."));
    } finally {
      setPendingDeleteId(null);
    }
  };

  const openLightbox = (index: number, event: React.SyntheticEvent) => {
    lightboxOpenerRef.current = event.currentTarget as HTMLElement;
    setLightboxIndex(index);
  };

  const onDrop = (event: React.DragEvent) => {
    event.preventDefault();
    dragDepthRef.current = 0;
    setDragActive(false);
    void handleFiles(event.dataTransfer.files);
  };

  const busy = uploading !== null;
  const activeImage = lightboxIndex !== null ? images[lightboxIndex] : undefined;
  const activeUrl = activeImage ? objectUrls[activeImage.id] : undefined;

  return (
    <section className="gallery-section" aria-labelledby="gallery-heading">
      <div className="gallery-header">
        <div className="gallery-heading-group">
          <span className="access-note-label" id="gallery-heading">
            Captures &amp; visuels
          </span>
          {images.length > 0 && <span className="gallery-count">{images.length}</span>}
        </div>
        <button
          type="button"
          className="btn btn-sm btn-secondary gallery-add-btn"
          onClick={() => fileInputRef.current?.click()}
          disabled={busy}
        >
          {busy ? `Envoi ${uploading.done + 1}/${uploading.total}…` : "+ Ajouter"}
        </button>
      </div>

      <input
        ref={fileInputRef}
        type="file"
        accept={ACCEPT_ATTRIBUTE}
        multiple
        className="gallery-file-input"
        onChange={(event) => {
          void handleFiles(event.target.files);
          // Reset so re-picking the same file still fires a change event.
          event.target.value = "";
        }}
      />

      <div
        className={`gallery-dropzone${dragActive ? " is-dragging" : ""}${busy ? " is-busy" : ""}`}
        onClick={() => !busy && fileInputRef.current?.click()}
        onKeyDown={(event) => {
          if (event.key === "Enter" || event.key === " ") {
            event.preventDefault();
            if (!busy) fileInputRef.current?.click();
          }
        }}
        onDragEnter={(event) => {
          event.preventDefault();
          dragDepthRef.current += 1;
          setDragActive(true);
        }}
        onDragOver={(event) => event.preventDefault()}
        onDragLeave={(event) => {
          event.preventDefault();
          dragDepthRef.current -= 1;
          if (dragDepthRef.current <= 0) {
            dragDepthRef.current = 0;
            setDragActive(false);
          }
        }}
        onDrop={onDrop}
        role="button"
        tabIndex={0}
        aria-label="Ajouter des images — glissez-déposez ou cliquez pour parcourir"
      >
        <div className="gallery-dropzone-icon" aria-hidden="true">
          {busy ? "⏳" : "🖼️"}
        </div>
        <div className="gallery-dropzone-text">
          {busy ? (
            <strong>Envoi en cours… {uploading.done}/{uploading.total}</strong>
          ) : (
            <>
              <strong>Glissez vos images ici</strong>
              <span> ou cliquez pour parcourir</span>
            </>
          )}
        </div>
        <div className="gallery-dropzone-hint">PNG · JPG · WEBP · GIF — 5 Mo maximum</div>
      </div>

      {error && (
        <div className="login-error error-banner gallery-error" role="alert">
          <span className="login-error-mark">!</span>
          <span>{error}</span>
        </div>
      )}

      {loading ? (
        <div className="gallery-empty">Chargement des images…</div>
      ) : images.length === 0 && !error ? (
        // Suppressed while an error is showing: "Aucune image pour ce client"
        // sitting under a failure banner reads as "your upload did nothing", which
        // is exactly the wrong conclusion when the upload was actually rejected.
        <div className="gallery-empty">
          Aucune image pour ce client — ajoutez une capture de configuration VPN, un
          schéma réseau ou une règle de pare-feu.
        </div>
      ) : images.length === 0 ? null : (
        <ul className="gallery-grid">
          {images.map((image, index) => {
            const url = objectUrls[image.id];
            return (
              <li className="gallery-tile" key={image.id}>
                <button
                  type="button"
                  className="gallery-thumb"
                  onClick={(event) => openLightbox(index, event)}
                  aria-label={`Agrandir ${image.fileName}`}
                  disabled={!url}
                >
                  {url ? (
                    <img src={url} alt={image.fileName} loading="lazy" />
                  ) : (
                    <span className="gallery-thumb-placeholder" aria-hidden="true" />
                  )}
                  <span className="gallery-thumb-overlay" aria-hidden="true">
                    ⤢
                  </span>
                </button>

                {confirmDeleteId === image.id ? (
                  <div className="gallery-confirm" role="alertdialog" aria-label="Confirmer la suppression">
                    <span className="gallery-confirm-text">Supprimer cette image ?</span>
                    <div className="gallery-confirm-actions">
                      <button
                        type="button"
                        className="gallery-confirm-btn gallery-confirm-yes"
                        onClick={() => void handleDelete(image)}
                        disabled={pendingDeleteId === image.id}
                      >
                        {pendingDeleteId === image.id ? "…" : "Supprimer"}
                      </button>
                      <button
                        type="button"
                        className="gallery-confirm-btn gallery-confirm-no"
                        onClick={() => setConfirmDeleteId(null)}
                      >
                        Annuler
                      </button>
                    </div>
                  </div>
                ) : (
                  <button
                    type="button"
                    className="gallery-delete"
                    onClick={() => setConfirmDeleteId(image.id)}
                    aria-label={`Supprimer ${image.fileName}`}
                    title="Supprimer"
                  >
                    ×
                  </button>
                )}

                <div className="gallery-tile-meta">
                  <span className="gallery-tile-name" title={image.fileName}>
                    {image.fileName}
                  </span>
                  <span className="gallery-tile-sub">
                    {formatBytes(image.sizeBytes)} · {formatDate(image.uploadedAt)}
                  </span>
                </div>
              </li>
            );
          })}
        </ul>
      )}

      {activeImage && (
        <div
          className="lightbox-backdrop"
          role="dialog"
          aria-modal="true"
          aria-label={`Aperçu de ${activeImage.fileName}`}
          onClick={closeLightbox}
        >
          <div className="lightbox-bar" onClick={(event) => event.stopPropagation()}>
            <div className="lightbox-title">
              <strong>{activeImage.fileName}</strong>
              <span>
                {formatBytes(activeImage.sizeBytes)} · {formatDate(activeImage.uploadedAt)}
              </span>
            </div>
            <div className="lightbox-actions">
              {images.length > 1 && (
                <span className="lightbox-counter">
                  {(lightboxIndex ?? 0) + 1} / {images.length}
                </span>
              )}
              {activeUrl && (
                <a
                  className="lightbox-btn"
                  href={activeUrl}
                  download={activeImage.fileName}
                  title="Télécharger"
                  aria-label={`Télécharger ${activeImage.fileName}`}
                >
                  ↓
                </a>
              )}
              <button
                ref={lightboxCloseRef}
                type="button"
                className="lightbox-btn lightbox-close"
                onClick={closeLightbox}
                aria-label="Fermer l'aperçu"
              >
                ×
              </button>
            </div>
          </div>

          {images.length > 1 && (
            <button
              type="button"
              className="lightbox-nav lightbox-prev"
              onClick={(event) => {
                event.stopPropagation();
                stepLightbox(-1);
              }}
              aria-label="Image précédente"
            >
              ‹
            </button>
          )}

          <div className="lightbox-stage" onClick={closeLightbox}>
            {activeUrl ? (
              <img
                className="lightbox-image"
                src={activeUrl}
                alt={activeImage.fileName}
                onClick={(event) => event.stopPropagation()}
              />
            ) : (
              <div className="lightbox-loading">Chargement…</div>
            )}
          </div>

          {images.length > 1 && (
            <button
              type="button"
              className="lightbox-nav lightbox-next"
              onClick={(event) => {
                event.stopPropagation();
                stepLightbox(1);
              }}
              aria-label="Image suivante"
            >
              ›
            </button>
          )}
        </div>
      )}
    </section>
  );
};
