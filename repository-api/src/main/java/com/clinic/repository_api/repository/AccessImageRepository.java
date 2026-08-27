package com.clinic.repository_api.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.clinic.repository_api.model.AccessImage;

public interface AccessImageRepository extends JpaRepository<AccessImage, Long> {

    /**
     * Closed interface projection: Spring Data derives a SELECT listing exactly these
     * columns, so the `data` BYTEA is left in the database. Returning the entity here
     * instead would pull every image's full payload into the heap just to render a
     * grid of thumbnails — a dozen 5 MB screenshots would be 60 MB per request.
     */
    interface AccessImageSummary {
        Long getId();

        String getFileName();

        String getContentType();

        long getSizeBytes();

        Instant getUploadedAt();
    }

    List<AccessImageSummary> findSummaryByClientIdOrderByUploadedAtDescIdDesc(Long clientId);

    /**
     * Scoped by clientId as well as id on purpose: the id alone is guessable, and
     * without the client predicate any authenticated user could walk the id space and
     * read images belonging to a client they were not looking at. Same reasoning as
     * TechnicalAccessRepository.findByIdAndClientId.
     */
    Optional<AccessImage> findByIdAndClientId(Long id, Long clientId);

    long countByClientId(Long clientId);

    /**
     * Bulk delete rather than JpaRepository.deleteById: that one loads the entity
     * first (SELECT ... including the BYTEA payload) only to discard it, so removing
     * a 5 MB image pulled 5 MB through the heap. This issues a single DELETE and
     * returns the affected row count, which doubles as the "did it exist?" answer and
     * removes the need for a preceding exists() round trip.
     */
    @Modifying
    @Query("DELETE FROM AccessImage i WHERE i.id = :id AND i.client.id = :clientId")
    int deleteByIdAndClientId(@Param("id") Long id, @Param("clientId") Long clientId);
}
