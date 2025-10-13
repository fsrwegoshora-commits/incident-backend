package com.smartincident.incidentbackend.incident.service;

import com.smartincident.incidentbackend.incident.dto.ChatMessageDto;
import com.smartincident.incidentbackend.incident.entity.ChatMessage;
import com.smartincident.incidentbackend.incident.entity.IncidentReport;
import com.smartincident.incidentbackend.incident.repository.ChatMessageRepository;
import com.smartincident.incidentbackend.incident.repository.IncidentReportRepository;
import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import com.smartincident.incidentbackend.enums.MessageType;
import com.smartincident.incidentbackend.utils.*;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    // private final NotificationService notificationService;  // For real-time notifications

    @Transactional
    public Response<ChatMessage> sendMessage(ChatMessageDto dto) {
        log.info(" Sending message for incident: {}", dto.getIncidentUid());

        if (dto.getIncidentUid() == null || dto.getIncidentUid().trim().isEmpty()) {
            return Response.error("Incident UID is required");
        }

        if (dto.getMessage() == null || dto.getMessage().trim().isEmpty()) {
            return Response.error("Message cannot be empty");
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

        // Verify sender has access to this incident
        boolean hasAccess = false;

        // Reporter can chat
        if (incident.getReportedBy().getUid().equals(senderUid)) {
            hasAccess = true;
        }

        // Assigned officer can chat
        if (incident.getAssignedOfficer() != null &&
                incident.getAssignedOfficer().getUserAccount().getUid().equals(senderUid)) {
            hasAccess = true;
        }

        // Station admin can chat
        if (sender.getRole().name().equals("STATION_ADMIN") ||
                sender.getRole().name().equals("ROOT")) {
            if (sender.getStation().getUid() != null &&
                    sender.getStation().getUid().equals(incident.getAssignedStation().getUid())) {
                hasAccess = true;
            }
        }

        if (!hasAccess) {
            return Response.error("You don't have access to this incident chat");
        }

        // Create message
        ChatMessage message = new ChatMessage();
        message.setSender(sender);
        message.setRelatedIncident(incident);
        message.setMessage(dto.getMessage());
        message.setMessageType(dto.getMessageType() != null ? dto.getMessageType() : MessageType.TEXT);
        message.setSentAt(LocalDateTime.now());

        try {
            message = chatMessageRepository.save(message);
            log.info(" Message sent successfully: {}", message.getUid());

            // TODO: Send real-time notification to other party
            // notificationService.notifyNewMessage(incident, message);

            return new Response<>(message+ "Message sent successfully");
        } catch (Exception e) {
            log.error(" Failed to send message: {}", e.getMessage());
            return Response.error("Failed to send message: " + Utils.getExceptionMessage(e));
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
            log.info("✅ System message sent successfully");
            return Response.success(chatMessage);
        } catch (Exception e) {
            log.error("Failed to send system message: {}", e.getMessage());
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
}