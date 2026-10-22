package ru.itmo.ticketing.user.common;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "User service",
                version = "0.2.0",
                description = "Пользователи и роли. Остальные сервисы узнают роль по GET /api/users/me "
                        + "с тем же заголовком X-User-Id, что прислал клиент."),
        security = @SecurityRequirement(name = CurrentUserResolver.HEADER))
@SecurityScheme(
        name = CurrentUserResolver.HEADER,
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = CurrentUserResolver.HEADER,
        description = "id пользователя из POST /api/users")
public class OpenApiConfig {

    static {
        SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentUser.class);
    }
}
