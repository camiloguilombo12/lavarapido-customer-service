package com.lavarapido.customer.infrastructure.adapter.in.messaging;

import com.lavarapido.customer.domain.port.in.ConsumeUserRegisteredUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class UserRegisteredListenerTest {

    private final ConsumeUserRegisteredUseCase consumer = mock(ConsumeUserRegisteredUseCase.class);
    private final UserRegisteredListener listener = new UserRegisteredListener(consumer);

    private static Message message(String json) {
        return new Message(json.getBytes(StandardCharsets.UTF_8), new MessageProperties());
    }

    @Test
    void createsTheProfileOfANewClient() {
        listener.onEvent(message("""
                {"eventId":"e1","eventType":"UserRegistered","aggregateId":"7","occurredAt":"2026-09-30T15:00:00Z",
                 "version":1,"payload":{"userId":7,"personId":33,"email":"a@b.co","firstName":"Ana","roles":["CLIENT"]}}
                """));
        verify(consumer).handle(7L, 33L);
    }

    @Test
    void ignoresAccountsThatAreNotClients() {
        listener.onEvent(message("""
                {"eventId":"e2","eventType":"UserRegistered","payload":{"userId":8,"personId":34,"roles":["OPERATOR"]}}
                """));
        verify(consumer, never()).handle(anyLong(), anyLong());
    }

    @Test
    void invalidMessagesGoToTheDeadLetterQueue() {
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> listener.onEvent(message("not json")));
        assertThrows(AmqpRejectAndDontRequeueException.class,
                () -> listener.onEvent(message("{\"eventId\":\"e3\",\"payload\":{}}")));
    }
}
