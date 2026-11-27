package de.omegazirkel.risingworld;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.function.Supplier;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.nio.AsyncPushConsumer;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.AsyncResponseConsumer;
import org.apache.hc.core5.http.nio.HandlerFactory;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.reactor.IOReactorStatus;
import org.apache.hc.core5.util.TimeValue;
import org.junit.Test;

import de.omegazirkel.risingworld.discordconnect.PluginSettings;

public class DiscordWebhookTransportTest {
    private static final URI WEBHOOK = URI.create("https://discord.invalid/webhook");

    @Test
    public void synchronousImageSubmissionFailureDoesNotBlockNextMessageOrShutdown() throws Exception {
        exercise(ImageOutcome.THROW);
    }

    @Test
    public void imageCallbackFailureDoesNotBlockNextMessageOrShutdown() throws Exception {
        exercise(ImageOutcome.FAIL);
    }

    @Test
    public void cancelledImageDoesNotBlockNextMessageOrShutdown() throws Exception {
        exercise(ImageOutcome.CANCEL);
    }

    @Test
    public void successfulImageAndTextOnlyMessagesCompleteInOrder() throws Exception {
        exercise(ImageOutcome.SUCCESS);
    }

    @Test(timeout = 8000)
    public void shutdownInterruptsAnOutstandingImageSubmission() throws Exception {
        DiscordConnect plugin = new DiscordConnect();
        FakeClient client = new FakeClient(ImageOutcome.PENDING);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), task -> {
                    Thread thread = new Thread(task, "webhook-shutdown-test");
                    thread.setDaemon(true);
                    return thread;
                });
        set(plugin, "discordTransportExecutor", executor);
        set(plugin, "webhookHttpClient", client);
        Field settings = DiscordConnectRuntime.class.getDeclaredField("s");
        settings.setAccessible(true);
        Object previousSettings = settings.get(null);
        settings.set(null, PluginSettings.getInstance());
        try {
            plugin.sendDiscordMessageToWebHook("test", "with image", WEBHOOK, new byte[] {1}, 0, null);
            assertTrue("image was not submitted", client.imageSubmitted.await(2, TimeUnit.SECONDS));
            Method shutdown = DiscordConnectRuntime.class.getDeclaredMethod("shutdownDiscordTransport");
            shutdown.setAccessible(true);
            shutdown.invoke(plugin);
            assertTrue("transport worker did not terminate", executor.awaitTermination(1, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
            settings.set(null, previousSettings);
        }
    }

    private void exercise(ImageOutcome outcome) throws Exception {
        DiscordConnect plugin = new DiscordConnect();
        FakeClient client = new FakeClient(outcome);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), task -> {
                    Thread thread = new Thread(task, "webhook-test");
                    thread.setDaemon(true);
                    return thread;
                });
        set(plugin, "discordTransportExecutor", executor);
        set(plugin, "webhookHttpClient", client);
        Field settings = DiscordConnectRuntime.class.getDeclaredField("s");
        settings.setAccessible(true);
        Object previousSettings = settings.get(null);
        settings.set(null, PluginSettings.getInstance());
        try {
            plugin.sendDiscordMessageToWebHook("test", "with image", WEBHOOK, new byte[] {1}, 0, null);
            plugin.sendDiscordMessageToWebHook("test", "next", WEBHOOK, null, 0, null);
            assertTrue("next queued text was never submitted", client.nextText.await(2, TimeUnit.SECONDS));
            Method shutdown = DiscordConnectRuntime.class.getDeclaredMethod("shutdownDiscordTransport");
            shutdown.setAccessible(true);
            shutdown.invoke(plugin);
            assertTrue("transport worker did not terminate", executor.awaitTermination(1, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
            settings.set(null, previousSettings);
        }
    }

    private static void set(Object instance, String name, Object value) throws Exception {
        Field field = DiscordConnectRuntime.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(instance, value);
    }

    private enum ImageOutcome { THROW, FAIL, CANCEL, SUCCESS, PENDING }

    private static final class FakeClient extends CloseableHttpAsyncClient {
        private final ImageOutcome outcome;
        private final AtomicInteger submissions = new AtomicInteger();
        private final CountDownLatch nextText = new CountDownLatch(1);
        private final CountDownLatch imageSubmitted = new CountDownLatch(1);

        private FakeClient(ImageOutcome outcome) {
            this.outcome = outcome;
        }

        @Override
        protected <T> Future<T> doExecute(HttpHost host, AsyncRequestProducer request,
                AsyncResponseConsumer<T> response, HandlerFactory<AsyncPushConsumer> push,
                HttpContext context, FutureCallback<T> callback) {
            int submission = submissions.incrementAndGet();
            if (submission == 2) {
                imageSubmitted.countDown();
                switch (outcome) {
                    case THROW -> throw new IllegalStateException("injected image submission failure");
                    case FAIL -> callback.failed(new IOException("injected image callback failure"));
                    case CANCEL -> callback.cancelled();
                    case SUCCESS -> complete(callback);
                    case PENDING -> { }
                }
            } else {
                complete(callback);
                if (submission == 3) nextText.countDown();
            }
            return CompletableFuture.completedFuture(null);
        }

        @SuppressWarnings("unchecked")
        private static <T> void complete(FutureCallback<T> callback) {
            callback.completed((T) SimpleHttpResponse.create(204));
        }

        @Override public void start() {}
        @Override public IOReactorStatus getStatus() { return IOReactorStatus.ACTIVE; }
        @Override public void awaitShutdown(TimeValue waitTime) {}
        @Override public void initiateShutdown() {}
        @Override public void register(String hostname, String uriPattern, Supplier<AsyncPushConsumer> supplier) {}
        @Override public void close(CloseMode closeMode) {}
        @Override public void close() {}
    }
}
