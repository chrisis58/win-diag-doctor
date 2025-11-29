package cn.teacy.wdd.support;

import lombok.Getter;

import java.util.concurrent.CompletableFuture;

@Getter
public class TaskHandle<T> {

    private final CompletableFuture<Void> ackFuture = new CompletableFuture<>();

    private final CompletableFuture<T> resultFuture = new CompletableFuture<>();

}
