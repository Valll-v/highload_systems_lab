package ru.itmo.ticketing.user.common;

import ru.itmo.ticketing.user.UserRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Пользователь из заголовка X-User-Id. Если указаны роли, подходит только одна из них.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
    UserRole[] value() default {};
}
