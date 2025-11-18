package com.smartincident.incidentbackend.incident.service;

import com.smartincident.incidentbackend.enums.IncidentStatus;
import com.smartincident.incidentbackend.enums.NotificationChannel;
import com.smartincident.incidentbackend.enums.NotificationType;
import com.smartincident.incidentbackend.incident.dto.ChatMessageDto;
import com.smartincident.incidentbackend.incident.entity.ChatMessage;
import com.smartincident.incidentbackend.incident.entity.IncidentReport;
import com.smartincident.incidentbackend.incident.repository.ChatMessageRepository;
import com.smartincident.incidentbackend.incident.repository.IncidentReportRepository;
import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import com.smartincident.incidentbackend.enums.MessageType;
import com.smartincident.incidentbackend.notification.dto.NotificationDto;
import com.smartincident.incidentbackend.notification.service.NotificationService;
import com.smartincident.incidentbackend.utils.*;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@GraphQLApi
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final IncidentReportRepository incidentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    // private final NotificationService notificationService;  // For real-time notifications

    @Transactional
    public Response<ChatMessage> sendMessage(ChatMessageDto dto) {
        log.info(" Sending message for incident: {}", dto.getIncidentUid());

        // Validation
        if (dto.getIncidentUid() == null || dto.getIncidentUid().trim().isEmpty()) {
            return Response.error("Incident UID is required");
        }

        // Enhanced validation based on message type
        ValidationResult validationResult = validateMessage(dto);
        if (!validationResult.isValid()) {
            return Response.error(validationResult.getErrorMessage());
        }

        String senderUid = LoggedUser.getUid();
        if (senderUid == null) {
            return Response.error("User not authenticated");
        }

        Optional<User> senderOpt = userRepository.findByUid(senderUid);
        if (!senderOpt.isPresent()) {
            return Response.error("Sender not found");
        }
        User sender = senderOpt.get();

        // Get incident
        Optional<IncidentReport> incidentOpt = incidentRepository.findByUid(dto.getIncidentUid());
        if (!incidentOpt.isPresent()) {
            return Response.error("Incident not found");
        }
        IncidentReport incident = incidentOpt.get();

        if(incident.getStatus()== IncidentStatus.RESOLVED)
            return Response.error("incident in already resolved");

        // Verify sender has access to this incident
        if (!verifyUserAccess(incident, sender)) {
            return Response.error("You don't have access to this incident chat");
        }

        // Create and save message
        try {
            ChatMessage message = createChatMessageFromDto(dto, sender, incident);
            message = chatMessageRepository.save(message);
            notifyChatMessage(message,incident);

            log.info("Message sent successfully: {}", message.getUid());
            return Response.success(message);

        } catch (Exception e) {
            log.error(" Failed to send message: {}", e.getMessage());
            return Response.error("Failed to send message: " + Utils.getExceptionMessage(e));
        }
    }

    // Helper method for validation
    private ValidationResult validateMessage(ChatMessageDto dto) {
        MessageType messageType = dto.getMessageType() != null ? dto.getMessageType() : MessageType.TEXT;

        switch (messageType) {
            case TEXT:
                if (dto.getMessage() == null || dto.getMessage().trim().isEmpty()) {
                    return ValidationResult.invalid("Message cannot be empty for TEXT type");
                }
                break;

            case IMAGE:
            case AUDIO:
            case VIDEO:
                if (dto.getMediaUrl() == null || dto.getMediaUrl().trim().isEmpty()) {
                    return ValidationResult.invalid("Media URL is required for " + messageType + " messages");
                }
                break;

            default:
                // SYSTEM messages might not need content validation
                break;
        }

        return ValidationResult.valid();
    }

    // Helper method to create ChatMessage from DTO
    private ChatMessage createChatMessageFromDto(ChatMessageDto dto, User sender, IncidentReport incident) {
        ChatMessage message = new ChatMessage();
        message.setSender(sender);
        message.setRelatedIncident(incident);

        // Set message content based on type
        if (dto.getMessageType() == MessageType.TEXT) {
            message.setMessage(dto.getMessage());
        } else {
            // For media messages, use filename or a descriptive text
            String displayMessage = dto.getMessage() != null ?
                    dto.getMessage() :
                    (dto.getMediaFileName() != null ?
                            dto.getMediaFileName() :
                            getDefaultMessageForType(dto.getMessageType()));
            message.setMessage(displayMessage);
        }

        message.setMessageType(dto.getMessageType() != null ? dto.getMessageType() : MessageType.TEXT);
        message.setSentAt(dto.getSentAt() != null ? dto.getSentAt() : LocalDateTime.now());

        // Set media properties
        message.setMediaUrl(dto.getMediaUrl());
        message.setMediaFileName(dto.getMediaFileName());
        message.setMediaFileSize(dto.getMediaFileSize());
        message.setMediaDuration(dto.getMediaDuration());
        message.setMediaThumbnailUrl(dto.getMediaThumbnailUrl());

        return message;
    }

    private String getDefaultMessageForType(MessageType messageType) {
        switch (messageType) {
            case IMAGE: return "📷 Image";
            case AUDIO: return "🎵 Audio";
            case VIDEO: return "🎥 Video";
            case SYSTEM: return "System Message";
            default: return "Message";
        }
    }

    // Access verification helper method
    private boolean verifyUserAccess(IncidentReport incident, User sender) {
        String senderUid = sender.getUid();

        // Reporter can chat
        if (incident.getReportedBy().getUid().equals(senderUid)) {
            return true;
        }

        // Assigned officer can chat
        if (incident.getAssignedOfficer() != null &&
                incident.getAssignedOfficer().getUserAccount() != null &&
                incident.getAssignedOfficer().getUserAccount().getUid().equals(senderUid)) {
            return true;
        }

        // Station admin or root can chat
        if (sender.getRole().name().equals("STATION_ADMIN") ||
                sender.getRole().name().equals("ROOT")) {
            if (sender.getStation() != null &&
                    sender.getStation().getUid() != null &&
                    incident.getAssignedStation() != null &&
                    sender.getStation().getUid().equals(incident.getAssignedStation().getUid())) {
                return true;
            }
        }

        return false;
    }

    // Validation helper class
    private static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }


    public ResponsePage<ChatMessage> getIncidentMessages(String incidentUid, PageableParam pageableParam) {
        log.info("Getting messages for incident: {}", incidentUid);

        if (incidentUid == null || incidentUid.trim().isEmpty()) {
            return new ResponsePage<>("Incident UID is required");
        }

        Optional<IncidentReport> incidentOpt = incidentRepository.findByUid(incidentUid);
        if (!incidentOpt.isPresent()) {
            return new ResponsePage<>("Incident not found");
        }

        IncidentReport incident = incidentOpt.get();

        // Verify user has access
        String currentUserUid = LoggedUser.getUid();
        boolean hasAccess = false;

        if (incident.getReportedBy().getUid().equals(currentUserUid)) {
            hasAccess = true;
        }

        if (incident.getAssignedOfficer() != null &&
                incident.getAssignedOfficer().getUserAccount().getUid().equals(currentUserUid)) {
            hasAccess = true;
        }

        // Check if admin
        Optional<User> currentUser = userRepository.findByUid(currentUserUid);
        if (currentUser.isPresent()) {
            String role = currentUser.get().getRole().name();
            if (role.equals("STATION_ADMIN") || role.equals("ROOT")) {
                hasAccess = true;
            }
        }

        if (!hasAccess) {
            return new ResponsePage<>("You don't have access to this incident chat");
        }

        Page<ChatMessage> messages = chatMessageRepository.findByIncidentPaginated(
                incidentUid,
                pageableParam.getPageable(true)
        );

        return new ResponsePage<>(messages);
    }


    public ResponseList<ChatMessage> getAllIncidentMessages(String incidentUid) {
        log.info("📋 Getting all messages for incident: {}", incidentUid);

        if (incidentUid == null || incidentUid.trim().isEmpty()) {
            return new ResponseList<>("Incident UID is required");
        }

        // Verify incident exists and user has access (same as above)
        Optional<IncidentReport> incidentOpt = incidentRepository.findByUid(incidentUid);
        if (!incidentOpt.isPresent()) {
            return new ResponseList<>("Incident not found");
        }

        IncidentReport incident = incidentOpt.get();
        String currentUserUid = LoggedUser.getUid();

        // Verify access (simplified)
        boolean hasAccess =
                incident.getReportedBy().getUid().equals(currentUserUid) ||
                        (incident.getAssignedOfficer() != null &&
                                incident.getAssignedOfficer().getUserAccount().getUid().equals(currentUserUid));

        if (!hasAccess) {
            // Check if admin
            Optional<User> currentUser = userRepository.findByUid(currentUserUid);
            if (currentUser.isPresent()) {
                String role = currentUser.get().getRole().name();
                hasAccess = role.equals("STATION_ADMIN") || role.equals("ROOT");
            }
        }

        if (!hasAccess) {
            return new ResponseList<>("You don't have access to this incident chat");
        }

        // Get all messages
        List<ChatMessage> messages = chatMessageRepository.findByIncident(incidentUid);

        return new ResponseList<>(messages);
    }


    public Response<ChatMessage> getMessage(String uid) {
        if (uid == null) {
            return Response.error("Message UID is required");
        }

        Optional<ChatMessage> message = chatMessageRepository.findByUid(uid);
        if (message.isPresent()) {
            return Response.success(message.get());
        }

        return Response.error("Message not found");
    }


    @Transactional
    public Response<ChatMessage> deleteMessage(String uid) {
        log.info(" Deleting message: {}", uid);

        if (uid == null) {
            return Response.error("Message UID is required");
        }

        Optional<ChatMessage> messageOpt = chatMessageRepository.findByUid(uid);
        if (!messageOpt.isPresent()) {
            return Response.error("Message not found");
        }

        ChatMessage message = messageOpt.get();

        // Verify user is the sender
        String currentUserUid = LoggedUser.getUid();
        if (!message.getSender().getUid().equals(currentUserUid)) {
            return Response.error("You can only delete your own messages");
        }

        // Check if message is recent (can only delete within 5 minutes)
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        if (message.getSentAt().isBefore(fiveMinutesAgo)) {
            return Response.error("Cannot delete messages older than 5 minutes");
        }

        message.delete();

        try {
            chatMessageRepository.save(message);
            log.info("✅ Message deleted successfully");
            return new Response<>(message+ "Message deleted successfully");
        } catch (Exception e) {
            log.error(" Failed to delete message: {}", e.getMessage());
            return Response.error("Failed to delete message: " + Utils.getExceptionMessage(e));
        }
    }


    @Transactional
    public Response<ChatMessage> sendSystemMessage(String incidentUid, String message) {
        log.info("🤖 Sending system message for incident: {}", incidentUid);

        Optional<IncidentReport> incidentOpt = incidentRepository.findByUid(incidentUid);
        if (!incidentOpt.isPresent()) {
            return Response.error("Incident not found");
        }

        IncidentReport incident = incidentOpt.get();

        // Use system user or reporter as sender
        User sender = incident.getReportedBy();

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSender(sender);
        chatMessage.setRelatedIncident(incident);
        chatMessage.setMessage(message);
        chatMessage.setMessageType(MessageType.SYSTEM);
        chatMessage.setSentAt(LocalDateTime.now());

        try {
            chatMessage = chatMessageRepository.save(chatMessage);
            log.info("System message sent successfully");
            return Response.success(chatMessage);
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error("Failed to send system message: " + Utils.getExceptionMessage(e));
        }
    }


    @Transactional
    public Response<Boolean> markMessagesAsRead(String incidentUid) {
        log.info(" Marking messages as read for incident: {}", incidentUid);

        // TODO: Implement read receipts
        // Add 'isRead' field to ChatMessage entity
        // Update all unread messages for current user

        return new Response<>(true+ "Messages marked as read");
    }


    public Response<Long> getUnreadMessageCount(String incidentUid) {
        log.info("📊 Getting unread message count for incident: {}", incidentUid);

        // TODO: Implement unread count
        // Query messages where isRead = false and recipient = current user

        return Response.success(0L);
    }

    private void notifyChatMessage(ChatMessage message, IncidentReport incident) {
        List<String> recipientUids = getChatRecipients(incident, message.getSender());

        if (!recipientUids.isEmpty()) {
            NotificationDto notificationDto = new NotificationDto();
            notificationDto.setTitle("New Message in Incident");
            notificationDto.setMessage(message.getSender().getName() + ": " +
                    (message.getMessage() != null ? message.getMessage() : "Sent a media file"));
            notificationDto.setType(NotificationType.CHAT_MESSAGE);

            notificationDto.setChannels(List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH));

            notificationDto.setRelatedEntityUid(incident.getUid());
            notificationDto.setRelatedEntityType("INCIDENT");
            notificationDto.setTargetUserUids(recipientUids);

            log.info("💬 Sending chat notification to {} users: {}", recipientUids.size(), recipientUids);
            notificationService.sendNotification(notificationDto);
        }
    }

    private List<String> getChatRecipients(IncidentReport incident, User sender) {
        List<String> recipients = new ArrayList<>();

        // Add reporter if not the sender
        if (!incident.getReportedBy().getUid().equals(sender.getUid())) {
            recipients.add(incident.getReportedBy().getUid());
        }

        // Add assigned officer if exists and not the sender
        if (incident.getAssignedOfficer() != null &&
                incident.getAssignedOfficer().getUserAccount() != null &&
                !incident.getAssignedOfficer().getUserAccount().getUid().equals(sender.getUid())) {
            recipients.add(incident.getAssignedOfficer().getUserAccount().getUid());
        }

        return recipients;
    }
}