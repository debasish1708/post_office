package com.postoffice.service;

import com.postoffice.dto.LetterResponse;
import com.postoffice.model.*;
import com.postoffice.repository.LetterRepository;
import com.postoffice.repository.PostalServiceRepository;
import com.postoffice.repository.UserRepository;
import com.postoffice.repository.WalletLedgerRepository;
import com.postoffice.route.RouteCatalog;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
public class LetterService {

    private static final BigDecimal UPGRADE_FEE = new BigDecimal("5.00");

    private final LetterRepository letterRepository;
    private final UserRepository userRepository;
    private final PostalServiceRepository postalServiceRepository;
    private final WalletLedgerRepository ledgerRepository;
    private final MailService mailService;
    private final RouteCatalog routeCatalog;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    public LetterService(LetterRepository letterRepository,
                         UserRepository userRepository,
                         PostalServiceRepository postalServiceRepository,
                         WalletLedgerRepository ledgerRepository,
                         MailService mailService,
                         RouteCatalog routeCatalog) {
        this.letterRepository = letterRepository;
        this.userRepository = userRepository;
        this.postalServiceRepository = postalServiceRepository;
        this.ledgerRepository = ledgerRepository;
        this.mailService = mailService;
        this.routeCatalog = routeCatalog;
    }

    @PostConstruct
    public void initUploads() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize uploads folder", e);
        }
    }

    public LetterResponse toResponse(Letter letter) {
        return LetterResponse.from(letter, routeCatalog.nodesFor(
                letter.getSender().getName(), letter.getReceiver().getName()));
    }

    @Transactional
    public Letter bookLetter(User sender, Long receiverId, String message,
                             MultipartFile letterFile, MultipartFile attachmentFile,
                             String serviceKey) throws IOException {
        if (letterFile == null || letterFile.isEmpty()) {
            throw new IllegalArgumentException("Letter file is required");
        }

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new IllegalArgumentException("Receiver not found"));
        if (sender.getId().equals(receiver.getId())) {
            throw new IllegalArgumentException("Cannot send a letter to yourself");
        }

        PostalService service = resolveService(serviceKey);
        BigDecimal charge = service.getCharges();
        if (sender.getBalance().compareTo(charge) < 0) {
            throw new IllegalStateException("Insufficient wallet balance. Charge: " + charge
                    + " coins, Balance: " + sender.getBalance());
        }

        sender.setBalance(sender.getBalance().subtract(charge));
        userRepository.save(sender);

        String letterImage = saveFile(letterFile);
        String attachmentImage = null;
        if (attachmentFile != null && !attachmentFile.isEmpty()) {
            attachmentImage = saveFile(attachmentFile);
        }

        LocalDateTime now = LocalDateTime.now();
        Letter letter = new Letter();
        letter.setSender(sender);
        letter.setReceiver(receiver);
        letter.setMessage(message);
        letter.setLetterImage(letterImage);
        letter.setAttachmentImage(attachmentImage);
        letter.setService(service);
        letter.setTotalCost(charge);
        letter.setPostDate(now);
        letter.setCreatedAt(now);
        letter.setReceivingDate(now.plusMinutes(service.getDeliveryMin()));
        letter.setCurrNodeAddress(routeCatalog.firstNode(sender.getName(), receiver.getName()));
        letter.setStatus(PostStatus.start);
        letter.setUnlockOtp(String.format("%06d", new Random().nextInt(1_000_000)));
        letter.setTrackingId(newTrackingId());
        letter = letterRepository.save(letter);

        addLedger(sender, letter, charge.negate(), "BOOKING",
                "Sent letter to " + receiver.getName() + " via " + service.getName());

        return letter;
    }

    @Transactional
    public Letter changeService(User sender, Long letterId, String newServiceKey) {
        Letter letter = letterRepository.findById(letterId)
                .orElseThrow(() -> new IllegalArgumentException("Letter not found"));

        if (!letter.getSender().getId().equals(sender.getId())) {
            throw new IllegalStateException("Only the sender can change the service");
        }
        if (!letter.isInTransit()) {
            throw new IllegalStateException("Service can only be updated while the letter is in transit");
        }

        PostalService newService = resolveService(newServiceKey);
        PostalService oldService = letter.getService();
        if (oldService.getId().equals(newService.getId())) {
            throw new IllegalArgumentException("Letter is already booked with " + newService.getName());
        }

        BigDecimal surcharge = UPGRADE_FEE.add(newService.getCharges());
        if (sender.getBalance().compareTo(surcharge) < 0) {
            throw new IllegalStateException("Insufficient balance. Upgrade cost: " + surcharge
                    + " coins (5 + new service). Balance: " + sender.getBalance());
        }

        sender.setBalance(sender.getBalance().subtract(surcharge));
        userRepository.save(sender);

        letter.setService(newService);
        letter.setTotalCost(letter.getTotalCost().add(surcharge));
        letter.setReceivingDate(letter.getPostDate().plusMinutes(newService.getDeliveryMin()));
        letter = letterRepository.save(letter);

        addLedger(sender, letter, surcharge.negate(), "UPGRADE",
                "Changed service from " + oldService.getName() + " to " + newService.getName()
                        + " (5 coin fee + " + newService.getCharges() + " new service)");

        if (newService.isSuperFast() && !LocalDateTime.now().isBefore(letter.getReceivingDate())) {
            deliverLetter(letter);
        }

        return letter;
    }

    @Transactional
    public Letter verifyOtpAndOpen(User receiver, Long letterId, String enteredOtp) {
        Letter letter = letterRepository.findById(letterId)
                .orElseThrow(() -> new IllegalArgumentException("Letter not found"));

        if (!letter.getReceiver().getId().equals(receiver.getId())) {
            throw new IllegalStateException("Only the receiver can unlock this letter");
        }
        if (letter.getStatus() != PostStatus.arrived) {
            throw new IllegalStateException("Letter has not arrived yet");
        }
        if (letter.isRead()) {
            return letter;
        }
        if (enteredOtp == null || !letter.getUnlockOtp().equals(enteredOtp.trim())) {
            throw new IllegalArgumentException("Invalid OTP. Access denied.");
        }

        letter.setRead(true);
        letter = letterRepository.save(letter);

        mailService.sendEmail(
                letter.getSender().getEmail(),
                "Letter read (" + letter.getTrackingId() + ")",
                String.format(
                        "Hello %s,%n%n%s has unlocked and downloaded your letter %s successfully.%n%nThank you for using PostOffice.",
                        letter.getSender().getName(),
                        letter.getReceiver().getName(),
                        letter.getTrackingId()));

        return letter;
    }

    @Transactional
    public Letter forceDeliver(User actor, Long letterId) {
        Letter letter = letterRepository.findById(letterId)
                .orElseThrow(() -> new IllegalArgumentException("Letter not found"));
        if (!letter.getSender().getId().equals(actor.getId())) {
            throw new IllegalStateException("Only the sender can simulate delivery");
        }
        if (!letter.isInTransit()) {
            throw new IllegalStateException("Letter is not in transit");
        }
        deliverLetter(letter);
        return letter;
    }

    @Transactional
    public void runDailyHops() {
        List<Letter> inTransit = letterRepository.findByStatusIn(List.of(PostStatus.start, PostStatus.inprocess));
        LocalDateTime now = LocalDateTime.now();
        for (Letter letter : inTransit) {
            if (letter.getService().isSuperFast()) {
                continue;
            }
            String sender = letter.getSender().getName();
            String receiver = letter.getReceiver().getName();
            String next = routeCatalog.nextNode(sender, receiver, letter.getCurrNodeAddress());
            letter.setCurrNodeAddress(next);
            letter.setStatus(PostStatus.inprocess);

            boolean last = routeCatalog.isLastNode(sender, receiver, next);
            boolean due = !now.isBefore(letter.getReceivingDate());
            if (last || due) {
                deliverLetter(letter);
            } else {
                letterRepository.save(letter);
            }
        }
    }

    @Transactional
    public void completeDueSuperfast() {
        List<Letter> due = letterRepository.findDueSuperfast(
                List.of(PostStatus.start, PostStatus.inprocess),
                "super_fast",
                LocalDateTime.now());
        for (Letter letter : due) {
            deliverLetter(letter);
        }
    }

    @Transactional
    public void resetMonthlyWallets() {
        BigDecimal allowance = new BigDecimal("100.00");
        for (User user : userRepository.findByDeletedAtIsNull()) {
            user.setBalance(allowance);
            userRepository.save(user);
            addLedger(user, null, allowance, "MONTHLY_RESET",
                    "Monthly wallet allowance set to 100 coins");
        }
    }

    public void deliverLetter(Letter letter) {
        if (letter.getStatus() == PostStatus.arrived) {
            return;
        }
        String last = routeCatalog.lastNode(letter.getSender().getName(), letter.getReceiver().getName());
        letter.setCurrNodeAddress(last);
        letter.setStatus(PostStatus.arrived);
        letterRepository.save(letter);

        mailService.sendEmail(
                letter.getSender().getEmail(),
                "Letter delivered (" + letter.getTrackingId() + ")",
                String.format(
                        "Hello %s,%n%nYour letter %s has been delivered successfully to %s.%n%nPostOffice Delivery Service",
                        letter.getSender().getName(),
                        letter.getTrackingId(),
                        letter.getReceiver().getName()));

        mailService.sendEmail(
                letter.getReceiver().getEmail(),
                "Unlock password for letter " + letter.getTrackingId(),
                String.format(
                        "Hello %s,%n%nA letter from %s has arrived.%nUse this 6-digit password to unlock and download it:%n%nOTP: %s%n%nTracking ID: %s%n%nPostOffice Delivery Service",
                        letter.getReceiver().getName(),
                        letter.getSender().getName(),
                        letter.getUnlockOtp(),
                        letter.getTrackingId()));
    }

    public Path getFilePath(String filename) {
        return Paths.get(uploadDir).resolve(filename).normalize();
    }

    private PostalService resolveService(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Service is required");
        }
        String trimmed = key.trim();
        return postalServiceRepository.findBySlug(trimmed)
                .or(() -> postalServiceRepository.findByNameIgnoreCase(trimmed))
                .or(() -> switch (trimmed.toLowerCase()) {
                    case "speed", "speed post", "speed_post" -> postalServiceRepository.findBySlug("speed_post");
                    case "superfast", "super_fast", "super fast" -> postalServiceRepository.findBySlug("super_fast");
                    case "normal", "normal_post" -> postalServiceRepository.findBySlug("normal_post");
                    default -> java.util.Optional.empty();
                })
                .orElseThrow(() -> new IllegalArgumentException("Invalid shipping service: " + key));
    }

    private String newTrackingId() {
        int year = LocalDateTime.now().getYear();
        String candidate;
        do {
            candidate = "PO-" + year + "-" + String.format("%05d", new Random().nextInt(100_000));
        } while (letterRepository.findByTrackingId(candidate).isPresent());
        return candidate;
    }

    private void addLedger(User user, Letter letter, BigDecimal amount, String type, String description) {
        WalletLedger row = new WalletLedger();
        row.setUser(user);
        row.setLetter(letter);
        row.setAmount(amount);
        row.setType(type);
        row.setDescription(description);
        ledgerRepository.save(row);
    }

    private String saveFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String newFilename = UUID.randomUUID() + fileExtension;
        Path targetPath = Paths.get(uploadDir).resolve(newFilename);
        Files.copy(file.getInputStream(), targetPath);
        return newFilename;
    }
}
