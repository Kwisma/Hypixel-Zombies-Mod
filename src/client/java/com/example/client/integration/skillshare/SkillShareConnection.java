package com.example.client.integration.skillshare;

import com.example.ZombiesMod;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** Thread-safe WebSocket transport. Minecraft state is never touched here. */
final class SkillShareConnection {

    private static final int INBOUND_CAPACITY = 512;

    private final HttpClient httpClient;
    private final URI endpoint;
    private final AtomicReference<WebSocket> socket = new AtomicReference<>();
    private final AtomicBoolean connecting = new AtomicBoolean();
    private final AtomicBoolean connectedSignal = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private final ConcurrentLinkedQueue<InboundMessage> inbound = new ConcurrentLinkedQueue<>();
    private final AtomicInteger inboundSize = new AtomicInteger();

    SkillShareConnection(HttpClient httpClient, URI endpoint) {
        this.httpClient = httpClient;
        this.endpoint = endpoint;
    }

    void connect() {
        if (isConnected() || !connecting.compareAndSet(false, true)) {
            return;
        }

        long expectedGeneration = generation.incrementAndGet();
        httpClient.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .buildAsync(endpoint, new Listener(expectedGeneration))
                .whenComplete((webSocket, error) -> {
                    connecting.set(false);
                    if (error != null && generation.get() == expectedGeneration) {
                        ZombiesMod.LOGGER.warn("SkillShare WebSocket connection failed", error);
                    }
                });
    }

    void disconnect() {
        generation.incrementAndGet();
        connecting.set(false);
        connectedSignal.set(false);
        clearInbound();
        WebSocket old = socket.getAndSet(null);
        if (old != null) {
            old.sendClose(WebSocket.NORMAL_CLOSURE, "leaving Zombies");
        }
    }

    boolean isConnected() {
        WebSocket current = socket.get();
        return current != null && !current.isInputClosed() && !current.isOutputClosed();
    }

    boolean consumeConnectedSignal() {
        return connectedSignal.getAndSet(false);
    }

    boolean send(String json) {
        WebSocket current = socket.get();
        if (current == null || current.isOutputClosed()) {
            return false;
        }
        try {
            current.sendText(json, true);
            return true;
        } catch (Exception error) {
            ZombiesMod.LOGGER.warn("SkillShare failed to send a WebSocket message", error);
            return false;
        }
    }

    String poll() {
        InboundMessage message;
        while ((message = inbound.poll()) != null) {
            inboundSize.updateAndGet(value -> Math.max(0, value - 1));
            if (message.generation == generation.get()) {
                return message.text;
            }
        }
        return null;
    }

    private void offer(long messageGeneration, String text) {
        inbound.offer(new InboundMessage(messageGeneration, text));
        if (inboundSize.incrementAndGet() > INBOUND_CAPACITY && inbound.poll() != null) {
            inboundSize.updateAndGet(value -> Math.max(0, value - 1));
        }
    }

    private void clearInbound() {
        inbound.clear();
        inboundSize.set(0);
    }

    private record InboundMessage(long generation, String text) {
    }

    private final class Listener implements WebSocket.Listener {
        private final long listenerGeneration;
        private final StringBuilder partialText = new StringBuilder();

        private Listener(long listenerGeneration) {
            this.listenerGeneration = listenerGeneration;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            if (generation.get() != listenerGeneration || !socket.compareAndSet(null, webSocket)) {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "stale connection");
                return;
            }
            clearInbound();
            connectedSignal.set(true);
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            if (generation.get() == listenerGeneration && socket.get() == webSocket) {
                partialText.append(data);
                if (last) {
                    offer(listenerGeneration, partialText.toString());
                    partialText.setLength(0);
                }
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            socket.compareAndSet(webSocket, null);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            if (socket.compareAndSet(webSocket, null)) {
                ZombiesMod.LOGGER.warn("SkillShare WebSocket closed with an error", error);
            }
        }
    }
}
