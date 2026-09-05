package com.postoffice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.postoffice.model.Letter;
import com.postoffice.model.PostalService;
import com.postoffice.model.PostStatus;
import com.postoffice.model.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class LetterResponse {
    private Long id;
    private String trackingId;
    private UserSummary sender;
    private UserSummary receiver;
    private String message;
    private String letterImage;
    private String attachmentImage;
    private ServiceSummary service;
    private BigDecimal totalCost;
    private LocalDateTime postDate;
    private LocalDateTime receivingDate;
    private String currNodeAddress;
    private PostStatus status;
    private boolean read;
    private List<String> routeNodes;

    public static LetterResponse from(Letter letter, List<String> routeNodes) {
        LetterResponse dto = new LetterResponse();
        dto.id = letter.getId();
        dto.trackingId = letter.getTrackingId();
        dto.sender = UserSummary.from(letter.getSender());
        dto.receiver = UserSummary.from(letter.getReceiver());
        dto.message = letter.getMessage();
        dto.letterImage = letter.getLetterImage();
        dto.attachmentImage = letter.getAttachmentImage();
        dto.service = ServiceSummary.from(letter.getService());
        dto.totalCost = letter.getTotalCost();
        dto.postDate = letter.getPostDate();
        dto.receivingDate = letter.getReceivingDate();
        dto.currNodeAddress = letter.getCurrNodeAddress();
        dto.status = letter.getStatus();
        dto.read = letter.isRead();
        dto.routeNodes = routeNodes;
        return dto;
    }

    public Long getId() { return id; }
    public String getTrackingId() { return trackingId; }
    public UserSummary getSender() { return sender; }
    public UserSummary getReceiver() { return receiver; }
    public String getMessage() { return message; }
    public String getLetterImage() { return letterImage; }
    public String getAttachmentImage() { return attachmentImage; }
    public ServiceSummary getService() { return service; }
    public BigDecimal getTotalCost() { return totalCost; }
    public LocalDateTime getPostDate() { return postDate; }
    public LocalDateTime getReceivingDate() { return receivingDate; }
    public String getCurrNodeAddress() { return currNodeAddress; }
    public PostStatus getStatus() { return status; }
    @JsonProperty("isRead")
    public boolean isRead() { return read; }
    public List<String> getRouteNodes() { return routeNodes; }

    public static class UserSummary {
        private Long id;
        private String name;
        private String email;

        public static UserSummary from(User user) {
            UserSummary s = new UserSummary();
            s.id = user.getId();
            s.name = user.getName();
            s.email = user.getEmail();
            return s;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
    }

    public static class ServiceSummary {
        private Long id;
        private String name;
        private String slug;
        private BigDecimal charges;
        private Integer deliveryMin;

        public static ServiceSummary from(PostalService service) {
            ServiceSummary s = new ServiceSummary();
            s.id = service.getId();
            s.name = service.getName();
            s.slug = service.getSlug();
            s.charges = service.getCharges();
            s.deliveryMin = service.getDeliveryMin();
            return s;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public String getSlug() { return slug; }
        public BigDecimal getCharges() { return charges; }
        public Integer getDeliveryMin() { return deliveryMin; }
    }
}
