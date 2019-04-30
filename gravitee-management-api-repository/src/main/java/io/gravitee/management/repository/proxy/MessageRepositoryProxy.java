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
package io.gravitee.management.repository.proxy;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MessageRepository;
import io.gravitee.repository.management.api.search.MessageCriteria;
import io.gravitee.repository.management.model.Message;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
@Component
public class MessageRepositoryProxy extends AbstractProxy<MessageRepository> implements MessageRepository {
    @Override
    public Optional<Message> findById(String id) throws TechnicalException {
        return target.findById(id);
    }

    @Override
    public Message create(Message message) throws TechnicalException {
        return target.create(message);
    }

    @Override
    public Message update(Message message) throws TechnicalException {
        return target.update(message);    }

    @Override
    public void delete(String id) throws TechnicalException {
        target.delete(id);
    }

    @Override
    public List<Message> search(MessageCriteria criteria) {
        return target.search(criteria);
    }
}
