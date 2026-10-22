package ru.itmo.ticketing.booking.common;

import ru.itmo.ticketing.booking.client.UserRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Пользователь из заголовка X-User-Id, роль спрашивается у user-service.
 * Если указаны роли, подходит только одна из них.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
    UserRole[] value() default {};
}
