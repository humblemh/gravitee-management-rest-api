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
package io.gravitee.management.services.search;

import io.gravitee.management.model.message.MessageEntity;
import io.gravitee.management.model.message.MessageTags;
import io.gravitee.management.service.MessageService;
import io.gravitee.management.service.search.SearchEngineService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ScheduledSearchIndexerServiceTest {

    @InjectMocks
    ScheduledSearchIndexerService service = new ScheduledSearchIndexerService();

    @Mock
    MessageService messageService;

    @Mock
    SearchEngineService searchEngineService;

    @Test
    public void shouldDoNothing() {
        when(messageService.search(any())).thenReturn(Collections.emptyList());

        service.run();

        verify(messageService, never()).ack(anyString());
        verify(searchEngineService, never()).readMessage(any());
    }


    @Test
    public void shouldInsertAndDelete() {
        MessageEntity insert = new MessageEntity();
        insert.setId("insertid");
        insert.setTags(Collections.singletonList(MessageTags.DATA_TO_INDEX));
        insert.setContent("{\"id\":\"1\"}");
        MessageEntity delete = new MessageEntity();
        delete.setId("deleteid");
        delete.setTags(Collections.singletonList(MessageTags.DATA_TO_INDEX));
        delete.setContent("{\"id\":\"2\"}");
        when(messageService.search(any())).thenReturn(Arrays.asList(delete, insert));

        service.run();

        verify(messageService, times(2)).ack(anyString());
        verify(searchEngineService, times(2)).readMessage(any());
    }
}
