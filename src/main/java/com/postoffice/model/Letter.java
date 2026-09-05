package com.postoffice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "post_office")
public class Letter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracking_id", nullable = false, unique = true, length = 40)
    private String trackingId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(name = "post_date", nullable = false,
            columnDefinition = "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime postDate = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "letter_image", nullable = false)
    private String letterImage;

    @Column(name = "attachment_image")
    private String attachmentImage;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "service_id", nullable = false)
    private PostalService service;

    @Column(name = "total_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCost;

    @Column(name = "receiving_date", nullable = false)
    private LocalDateTime receivingDate;

    @Column(name = "curr_node_address", nullable = false, length = 150)
    private String currNodeAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status = PostStatus.start;

    @JsonIgnore
    @Column(name = "unlock_otp", nullable = false, length = 6)
    private String unlockOtp;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false,
            columnDefinition = "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isInTransit() {
        return status == PostStatus.start || status == PostStatus.inprocess;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTrackingId() { return trackingId; }
    public void setTrackingId(String trackingId) { this.trackingId = trackingId; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public User getReceiver() { return receiver; }
    public void setReceiver(User receiver) { this.receiver = receiver; }

    public LocalDateTime getPostDate() { return postDate; }
    public void setPostDate(LocalDateTime postDate) { this.postDate = postDate; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getLetterImage() { return letterImage; }
    public void setLetterImage(String letterImage) { this.letterImage = letterImage; }

    public String getAttachmentImage() { return attachmentImage; }
    public void setAttachmentImage(String attachmentImage) { this.attachmentImage = attachmentImage; }

    public PostalService getService() { return service; }
    public void setService(PostalService service) { this.service = service; }

    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }

    public LocalDateTime getReceivingDate() { return receivingDate; }
    public void setReceivingDate(LocalDateTime receivingDate) { this.receivingDate = receivingDate; }

    public String getCurrNodeAddress() { return currNodeAddress; }
    public void setCurrNodeAddress(String currNodeAddress) { this.currNodeAddress = currNodeAddress; }

    public PostStatus getStatus() { return status; }
    public void setStatus(PostStatus status) { this.status = status; }

    public String getUnlockOtp() { return unlockOtp; }
    public void setUnlockOtp(String unlockOtp) { this.unlockOtp = unlockOtp; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
