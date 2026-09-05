package com.postoffice.repository;

import com.postoffice.model.Letter;
import com.postoffice.model.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LetterRepository extends JpaRepository<Letter, Long> {
    Optional<Letter> findByTrackingId(String trackingId);

    List<Letter> findBySenderIdAndDeletedAtIsNullOrderByPostDateDesc(Long senderId);

    List<Letter> findByReceiverIdAndDeletedAtIsNullOrderByPostDateDesc(Long receiverId);

    List<Letter> findByStatusIn(List<PostStatus> statuses);

    @Query("SELECT l FROM Letter l WHERE l.status IN :statuses AND l.service.slug = :slug AND l.receivingDate <= :now")
    List<Letter> findDueSuperfast(@Param("statuses") List<PostStatus> statuses,
                                  @Param("slug") String slug,
                                  @Param("now") LocalDateTime now);
}
