package ru.itmo.ticketing.booking.common;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Callable;

/**
 * JPA и Feign блокирующие, поэтому их вызовы уходят с event loop Netty
 * на пул boundedElastic, а наружу отдаётся Mono.
 */
public final class Blocking {

    private Blocking() {
    }

    public static <T> Mono<T> call(Callable<T> call) {
        return Mono.fromCallable(call).subscribeOn(Schedulers.boundedElastic());
    }

    public static Mono<Void> run(Runnable action) {
        return Mono.<Void>fromRunnable(action).subscribeOn(Schedulers.boundedElastic());
    }
}
