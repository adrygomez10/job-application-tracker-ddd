package dev.adriangm.tracker.search.adapter.feed;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/** Bound the response while receiving it, before allocating or parsing an unbounded body. */
final class LimitedJsonBody implements HttpResponse.BodyHandler<byte[]> {
    @Override
    public HttpResponse.BodySubscriber<byte[]> apply(HttpResponse.ResponseInfo response)
    {
        return new Subscriber();
    }

    private static final class Subscriber implements HttpResponse.BodySubscriber<byte[]> {
        private static final int MAX_BYTES = 2 * 1024 * 1024;
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private final CompletableFuture<byte[]> result = new CompletableFuture<>();
        private Flow.Subscription subscription;

        @Override
        public CompletionStage<byte[]> getBody()
        {
            return result;
        }

        @Override
        public void onSubscribe(Flow.Subscription next)
        {
            subscription = next;
            next.request(1);
        }

        @Override
        public void onNext(List<ByteBuffer> buffers)
        {
            for (ByteBuffer buffer : buffers) {
                if (buffer.remaining() > MAX_BYTES - bytes.size()) {
                    subscription.cancel();
                    result.completeExceptionally(new IOException("Job feed exceeds 2 MiB"));
                    return;
                }
                byte[] chunk = new byte[buffer.remaining()];
                buffer.get(chunk);
                bytes.writeBytes(chunk);
            }
            subscription.request(1);
        }

        @Override
        public void onError(Throwable failure)
        {
            result.completeExceptionally(failure);
        }

        @Override
        public void onComplete()
        {
            result.complete(bytes.toByteArray());
        }
    }
}
