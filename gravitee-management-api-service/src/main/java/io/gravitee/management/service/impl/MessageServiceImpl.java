/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.management.service.impl;

import io.gravitee.common.utils.UUID;
import io.gravitee.management.model.message.MessageEntity;
import io.gravitee.management.model.message.MessageQuery;
import io.gravitee.management.model.message.MessageTags;
import io.gravitee.management.model.message.NewMessageEntity;
import io.gravitee.management.service.MessageService;
import io.gravitee.management.service.exceptions.Message2RecipientNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.node.api.Node;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MessageRepository;
import io.gravitee.repository.management.api.search.MessageCriteria;
import io.gravitee.repository.management.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MessageServiceImpl extends AbstractService implements MessageService {

    private final Logger logger = LoggerFactory.getLogger(MessageServiceImpl.class);

    @Autowired
    MessageRepository messageRepository;

    @Autowired
    Node node;

    @Override
    public void send(NewMessageEntity messageEntity) {
        if (messageEntity.getTo() == null || messageEntity.getTo().isEmpty()) {
            throw new Message2RecipientNotFoundException();
        }

        Message message = new Message();
        message.setId(UUID.toString(java.util.UUID.randomUUID()));
        message.setFrom(node.id());
        message.setTo(messageEntity.getTo());
        message.setTags(convert(messageEntity.getTags()));
        long now = System.currentTimeMillis();
        message.setCreatedAt(new Date(now));
        message.setUpdatedAt(message.getCreatedAt());
        message.setDeleteAt(new Date(now + (messageEntity.getTtlInSeconds() * 1000)));
        if (messageEntity.getContent() != null) {
            message.setContent(messageEntity.getContent());
        }

        try {
            messageRepository.create(message);
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to create {}", message, ex);
            throw new TechnicalManagementException("An error occurs while trying create " + message, ex);
        }
    }

    @Override
    public List<MessageEntity> search(MessageQuery query) {
        //convert tags
        String[] tags = null;
        if (query.getTags() != null) {
            tags = query.getTags()
                    .stream()
                    .map(Enum::name)
                    .toArray(String[]::new);
        }
        MessageCriteria criteria = new MessageCriteria.Builder()
                .to(query.getTo())
                .tags(tags)
                .notAckBy(node.id())
                .notDeleted()
                .build();
        return messageRepository.search(criteria)
                .stream()
                .map(this::map)
                .collect(Collectors.toList());
    }

    @Override
    public void ack(String messageId) {
        try {
            Optional<Message> optMsg = messageRepository.findById(messageId);
            //if not found, this is probably because it has been deleted
            if (optMsg.isPresent()) {
                Message msg = optMsg.get();
                if (msg.getAcknowledgments() == null) {
                    msg.setAcknowledgments(Collections.singletonList(node.id()));
                } else {
                    msg.getAcknowledgments().add(node.id());
                }
                messageRepository.update(msg);
            }
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to acknowledge a message", ex);
        }
    }

    private List<String> convert(List<io.gravitee.management.model.message.MessageTags> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }

        return tags
                .stream()
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    private MessageEntity map(Message message) {
        if (message == null) {
            return null;
        }

        MessageEntity messageEntity = new MessageEntity();

        messageEntity.setId(message.getId());
        messageEntity.setTo(message.getTo());
        messageEntity.setContent(message.getContent());
        if (message.getTags() != null && !message.getTags().isEmpty()) {
            messageEntity.setTags(
                    message.getTags()
                            .stream()
                            .map(MessageTags::valueOf)
                            .collect(Collectors.toList()));
        }

        return messageEntity;
    }
}
