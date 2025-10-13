package com.smartincident.incidentbackend.incident.controller;

import com.smartincident.incidentbackend.incident.dto.ChatMessageDto;
import com.smartincident.incidentbackend.incident.entity.ChatMessage;
import com.smartincident.incidentbackend.incident.service.ChatMessageService;
import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.AuthorizedRole;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.utils.*;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@GraphQLApi
@RequiredArgsConstructor
@Slf4j
public class ChatMessageController {

    private final ChatMessageService chatMessageService;


    @Authenticated
    @GraphQLMutation(name = "sendChatMessage", description = "Send a message in incident chat")
    public Response<ChatMessage> sendMessage(@GraphQLArgument(name = "chatMessageDto") ChatMessageDto dto) {
        log.info(" Sending chat message for incident: {}", dto.getIncidentUid());
        return chatMessageService.sendMessage(dto);
    }


    @Authenticated
    @GraphQLMutation(name = "deleteChatMessage", description = "Delete a chat message")
    public Response<ChatMessage> deleteMessage(@GraphQLArgument(name = "uid") String uid) {
        log.info("🗑 Deleting chat message: {}", uid);
        return chatMessageService.deleteMessage(uid);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT})
    @GraphQLMutation(name = "sendSystemMessage", description = "Send automated system message")
    public Response<ChatMessage> sendSystemMessage(@GraphQLArgument(name = "incidentUid") String incidentUid, @GraphQLArgument(name = "message") String message) {
        log.info("🤖 Sending system message");
        return chatMessageService.sendSystemMessage(incidentUid, message);
    }

    @Authenticated
    @GraphQLMutation(name = "markMessagesAsRead", description = "Mark messages as read")
    public Response<Boolean> markMessagesAsRead(@GraphQLArgument(name = "incidentUid") String incidentUid) {
        log.info(" Marking messages as read");
        return chatMessageService.markMessagesAsRead(incidentUid);
    }

    @Authenticated
    @GraphQLQuery(name = "getChatMessage", description = "Get a single chat message by UID")
    public Response<ChatMessage> getMessage(@GraphQLArgument(name = "uid") String uid) {
        return chatMessageService.getMessage(uid);
    }

    @Authenticated
    @GraphQLQuery(name = "getIncidentMessages", description = "Get paginated chat messages for an incident")
    public ResponsePage<ChatMessage> getIncidentMessages(@GraphQLArgument(name = "incidentUid") String incidentUid, @GraphQLArgument(name = "pageableParam") PageableParam pageableParam) {
        log.info(" Getting incident messages (paginated)");
        return chatMessageService.getIncidentMessages(incidentUid, pageableParam);
    }


    @Authenticated
    @GraphQLQuery(name = "getAllIncidentMessages", description = "Get all chat messages for an incident")
    public ResponseList<ChatMessage> getAllIncidentMessages(@GraphQLArgument(name = "incidentUid") String incidentUid) {
        log.info(" Getting all incident messages");
        return chatMessageService.getAllIncidentMessages(incidentUid);
    }

    @Authenticated
    @GraphQLQuery(name = "getUnreadMessageCount", description = "Get count of unread messages")
    public Response<Long> getUnreadMessageCount(@GraphQLArgument(name = "incidentUid") String incidentUid) {
        return chatMessageService.getUnreadMessageCount(incidentUid);
    }
}