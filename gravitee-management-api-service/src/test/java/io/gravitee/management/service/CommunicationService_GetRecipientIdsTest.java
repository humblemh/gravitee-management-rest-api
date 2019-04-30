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
package io.gravitee.management.service;

import io.gravitee.management.model.communication.CommunicationChannel;
import io.gravitee.management.model.communication.CommunicationEntity;
import io.gravitee.management.model.communication.CommunicationRecipientEntity;
import io.gravitee.management.service.exceptions.MessageRecipientFormatException;
import io.gravitee.management.service.impl.CommunicationServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.model.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Set;

import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class CommunicationService_GetRecipientIdsTest {

    @InjectMocks
    private CommunicationServiceImpl messageService = new CommunicationServiceImpl();

    @Mock
    ApiRepository mockApiRepository;

    @Mock
    MembershipRepository mockMembershipRepository;

    @Mock
    SubscriptionRepository mockSubscriptionRepository;

    @Test
    public void shouldThrowExceptionIfNull() {
        shouldThrowException(null, null);
        shouldThrowException("xxx", null);

        CommunicationEntity communicationEntity = new CommunicationEntity();
        shouldThrowException(null, communicationEntity);
        shouldThrowException("xxx", communicationEntity);

        CommunicationRecipientEntity communicationRecipientEntity = new CommunicationRecipientEntity();
        communicationEntity.setRecipient(communicationRecipientEntity);
        shouldThrowException("xxx", communicationEntity);

        communicationRecipientEntity.setRoleScope("API");
        communicationRecipientEntity.setRoleValues(Collections.emptyList());
        shouldThrowException("xxx", communicationEntity);
    }

    private void shouldThrowException(String apiId, CommunicationEntity message) {
        try {
            Api api = new Api();
            api.setId(apiId);
            messageService.getRecipientsId(api, message);
            fail("should throw MessageRecipientFormatException");
        } catch(MessageRecipientFormatException ex) {
            // ok
        }
    }

    @Test
    public void shouldNotGetGlobal() throws Exception {
        shouldNotGetGlobal("API");
        shouldNotGetGlobal("APPLICATION");
        shouldNotGetGlobal("PORTAL");
    }

    private void shouldNotGetGlobal(String scope) throws Exception {

        CommunicationEntity communicationEntity = new CommunicationEntity();
        communicationEntity.setChannel(CommunicationChannel.MAIL);
        CommunicationRecipientEntity communicationRecipientEntity = new CommunicationRecipientEntity();
        communicationRecipientEntity.setRoleScope(scope);
        communicationRecipientEntity.setRoleValues(Collections.singletonList("API_PUBLISHER"));
        communicationEntity.setRecipient(communicationRecipientEntity);

        messageService.getRecipientsId(communicationEntity);

        verify(mockMembershipRepository, never()).findByRole(any(), any());
        verify(mockApiRepository, never()).findById(any());
        verify(mockSubscriptionRepository, never()).search(any());
        verify(mockMembershipRepository, never()).findByReferencesAndRole(any(), any(), any(), any());
    }

    @Test
    public void shouldGetGlobalAPIPublisher() throws Exception {
        CommunicationEntity communicationEntity = new CommunicationEntity();
        communicationEntity.setChannel(CommunicationChannel.MAIL);
        CommunicationRecipientEntity communicationRecipientEntity = new CommunicationRecipientEntity();
        communicationRecipientEntity.setRoleScope("MANAGEMENT");
        communicationRecipientEntity.setRoleValues(Collections.singletonList("API_PUBLISHER"));
        communicationEntity.setRecipient(communicationRecipientEntity);
        Membership membership = new Membership();
        membership.setUserId("user-id");
        when(mockMembershipRepository.findByRole(RoleScope.MANAGEMENT, "API_PUBLISHER"))
                .thenReturn(Collections.singleton(membership));

        Set<String> recipientIds = messageService.getRecipientsId(communicationEntity);

        assertNotNull("not null", recipientIds);
        assertEquals("size=1", 1, recipientIds.size());
        assertTrue("user=user-id", recipientIds.contains("user-id"));
        verify(mockMembershipRepository, times(1)).findByRole(RoleScope.MANAGEMENT, "API_PUBLISHER");
        verify(mockApiRepository, never()).findById(any());
        verify(mockSubscriptionRepository, never()).search(any());
        verify(mockMembershipRepository, never()).findByReferencesAndRole(any(), any(), any(), any());
    }

    @Test
    public void shouldNotGetSpecific() throws Exception {
        shouldNotGetSpecific("API");
        shouldNotGetSpecific("PORTAL");
        shouldNotGetSpecific("MANAGEMENT");
    }

    private void shouldNotGetSpecific(String scope) throws Exception {
        Api api = new Api();
        api.setId("api-id");
        CommunicationEntity communicationEntity = new CommunicationEntity();
        communicationEntity.setChannel(CommunicationChannel.MAIL);
        CommunicationRecipientEntity communicationRecipientEntity = new CommunicationRecipientEntity();
        communicationRecipientEntity.setRoleScope(scope);
        communicationRecipientEntity.setRoleValues(Collections.singletonList("API_PUBLISHER"));
        communicationEntity.setRecipient(communicationRecipientEntity);

        messageService.getRecipientsId(api, communicationEntity);

        verify(mockMembershipRepository, never()).findByRole(any(), any());
        verify(mockApiRepository, never()).findById(any());
        verify(mockSubscriptionRepository, never()).search(any());
        verify(mockMembershipRepository, never()).findByReferencesAndRole(any(), any(), any(), any());
    }

    @Test
    public void shouldGetApiConsumersWithoutGroups() throws TechnicalException {
        Api api = new Api();
        api.setId("api-id");
        api.setGroups(Collections.emptySet());
        CommunicationEntity communicationEntity = new CommunicationEntity();
        communicationEntity.setChannel(CommunicationChannel.MAIL);
        CommunicationRecipientEntity communicationRecipientEntity = new CommunicationRecipientEntity();
        communicationRecipientEntity.setRoleScope("APPLICATION");
        communicationRecipientEntity.setRoleValues(Collections.singletonList("OWNER"));
        communicationEntity.setRecipient(communicationRecipientEntity);
        Membership membership = new Membership();
        membership.setUserId("user-id");
        when(mockApiRepository.findById("api-id"))
                .thenReturn(of(api));
        Subscription subscription = new Subscription();
        subscription.setApplication("app-id");
        when(mockSubscriptionRepository.search(any()))
                .thenReturn(Collections.singletonList(subscription));
        when(mockMembershipRepository.findByReferencesAndRole(eq(MembershipReferenceType.APPLICATION), any(), any(), any()))
                .thenReturn(Collections.singleton(membership));

        Set<String> recipientIds = messageService.getRecipientsId(api, communicationEntity);

        assertNotNull("not null", recipientIds);
        assertEquals("size=1", 1, recipientIds.size());
        assertTrue("user=user-id", recipientIds.contains("user-id"));
        verify(mockMembershipRepository, never()).findByRole(any(), any());
        verify(mockSubscriptionRepository, times(1)).search(any());
        verify(mockMembershipRepository, never()).findByReferencesAndRole(eq(MembershipReferenceType.GROUP), any(), any(), any());
        verify(mockMembershipRepository, times(1)).findByReferencesAndRole(eq(MembershipReferenceType.APPLICATION), any(), any(), any());
    }

    @Test
    public void shouldGetApiConsumersWithGroups() throws TechnicalException {
        Api api = new Api();
        api.setId("api-id");
        api.setGroups(Collections.singleton("group-id"));
        CommunicationEntity communicationEntity = new CommunicationEntity();
        communicationEntity.setChannel(CommunicationChannel.MAIL);
        CommunicationRecipientEntity communicationRecipientEntity = new CommunicationRecipientEntity();
        communicationRecipientEntity.setRoleScope("APPLICATION");
        communicationRecipientEntity.setRoleValues(Collections.singletonList("OWNER"));
        communicationEntity.setRecipient(communicationRecipientEntity);
        Membership membershipGroup = new Membership();
        membershipGroup.setUserId("user-group-id");
        Membership membership = new Membership();
        membership.setUserId("user-id");
        when(mockApiRepository.findById("api-id"))
                .thenReturn(of(api));
        Subscription subscription = new Subscription();
        subscription.setApplication("app-id");
        when(mockSubscriptionRepository.search(any()))
                .thenReturn(Collections.singletonList(subscription));
        when(mockMembershipRepository.findByReferencesAndRole(eq(MembershipReferenceType.APPLICATION), any(), any(), any()))
                .thenReturn(Collections.singleton(membership));
        when(mockMembershipRepository.findByReferencesAndRole(eq(MembershipReferenceType.GROUP), any(), any(), any()))
                .thenReturn(Collections.singleton(membershipGroup));

        Set<String> recipientIds = messageService.getRecipientsId(api, communicationEntity);

        assertNotNull("not null", recipientIds);
        assertEquals("size=2", 2, recipientIds.size());
        assertTrue("user=user-id", recipientIds.contains("user-id"));
        assertTrue("user=user-group-id", recipientIds.contains("user-group-id"));
        verify(mockMembershipRepository, never()).findByRole(any(), any());
        verify(mockSubscriptionRepository, times(1)).search(any());
        verify(mockMembershipRepository, times(1)).findByReferencesAndRole(eq(MembershipReferenceType.GROUP), any(), any(), any());
        verify(mockMembershipRepository, times(1)).findByReferencesAndRole(eq(MembershipReferenceType.APPLICATION), any(), any(), any());
    }
}
