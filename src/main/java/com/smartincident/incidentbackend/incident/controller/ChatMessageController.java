package com.smartincident.incidentbackend.incident.controller;

import com.smartincident.incidentbackend.incident.dto.ChatMessageDto;
import com.smartincident.incidentbackend.incident.entity.ChatMessage;
import com.smartincident.incidentbackend.incident.service.ChatMessageService;
import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.RequiresPermission;
import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat/messages")
@RequiredArgsConstructor
@Slf4j
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    /** Send a message on an incident channel. Open to all operational roles — Dispatchers, Officers, and Station Admins. */
    @Authenticated
    @RequiresPermission(Permission.SEND_CHAT_MESSAGE)
    @PostMapping
    public Response<ChatMessage> sendMessage(@RequestBody ChatMessageDto dto) {
        log.info("Sending chat message for incident: {}", dto.getIncidentUid());
        return chatMessageService.sendMessage(dto);
    }

    @Authenticated
    @DeleteMapping("/{uid}")
    public Response<ChatMessage> deleteMessage(@PathVariable String uid) {
        log.info("Deleting chat message: {}", uid);
        return chatMessageService.deleteMessage(uid);
    }

    @Authenticated
    @RequiresPermission(Permission.MANAGE_CHAT_MESSAGES)
    @PostMapping("/system")
    public Response<ChatMessage> sendSystemMessage(@RequestParam String incidentUid, @RequestParam String message) {
        log.info("Sending system message");
        return chatMessageService.sendSystemMessage(incidentUid, message);
    }

    @Authenticated
    @PutMapping("/read/{incidentUid}")
    public Response<Boolean> markMessagesAsRead(@PathVariable String incidentUid) {
        log.info(" Marking messages as read");
        return chatMessageService.markMessagesAsRead(incidentUid);
    }

    @Authenticated
    @GetMapping("/{uid}")
    public Response<ChatMessage> getMessage(@PathVariable String uid) {
        return chatMessageService.getMessage(uid);
    }

    @Authenticated
    @GetMapping("/incident/{incidentUid}")
    public ResponsePage<ChatMessage> getIncidentMessages(@PathVariable String incidentUid, @ModelAttribute PageableParam pageableParam) {
        log.info(" Getting incident messages (paginated)");
        return chatMessageService.getIncidentMessages(incidentUid, pageableParam);
    }

    @Authenticated
    @GetMapping("/incident/{incidentUid}/all")
    public ResponseList<ChatMessage> getAllIncidentMessages(@PathVariable String incidentUid) {
        log.info(" Getting all incident messages");
        return chatMessageService.getAllIncidentMessages(incidentUid);
    }

    @Authenticated
    @GetMapping("/incident/{incidentUid}/unread-count")
    public Response<Long> getUnreadMessageCount(@PathVariable String incidentUid) {
        return chatMessageService.getUnreadMessageCount(incidentUid);
    }
}
