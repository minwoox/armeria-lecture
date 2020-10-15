package armeria.lecture.week1;

import static org.awaitility.Awaitility.await;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

class FutureAndThreadTest {

    @Test
    void simpleCallback() {
        currentThreadName();
        final CompletableFuture<String> future = new CompletableFuture<>();
        future.thenAccept(str -> {
            System.err.println("Hello " + str);
            currentThreadName();
        });

        future.complete("Armeria");
    }

    @Test
    void futureGet() throws Exception {
        currentThreadName();
        final CompletableFuture<String> future = new CompletableFuture<>();
        // Never complete
        // future.get();

        future.complete("Armeria");
    }

    @Test
    void completeByAnotherThread() {
        currentThreadName();
        final CompletableFuture<String> future = new CompletableFuture<>();
        future.thenAccept(str -> {
            System.err.println("Hello " + str);
            currentThreadName();
        });

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            future.complete("foo");
        });

//        await().until(future::isDone);
    }

    @Test
    void completeByAnotherThread_completeFirst() {
        currentThreadName();
        final CompletableFuture<String> future = new CompletableFuture<>();
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            future.complete("foo");
        });

        future.thenAccept(str -> {
            currentThreadName();
        });

        await().until(future::isDone);
    }

    @Test
    void completeByAnotherThread_thenAcceptAsync() {
        currentThreadName();
        final CompletableFuture<String> future = new CompletableFuture<>();
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        future.thenAcceptAsync(str -> {
            currentThreadName();
        }, executor);

        executor.submit(() -> {
            future.complete("foo");
        });

        await().until(future::isDone);
    }

    @Test
    void completeByAnotherThread_thenAcceptAsync_forkJoin() {
        currentThreadName();
        final CompletableFuture<String> future = new CompletableFuture<>();
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        future.thenAcceptAsync(str -> {
            currentThreadName();
        });

        executor.submit(() -> {
            future.complete("foo");
        });

        await().until(future::isDone);
    }

    static void currentThreadName() {
        System.err.println("Name: " + Thread.currentThread().getName());
    }
}
