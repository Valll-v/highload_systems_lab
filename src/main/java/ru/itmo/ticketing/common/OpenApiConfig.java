package ru.itmo.ticketing.common;

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
                title = "Ticketing API",
                version = "0.1.0",
                description = "Платформа бронирования билетов. Пользователь передаётся заголовком X-User-Id, "
                        + "роль берётся из базы. Списки отдаются страницами не больше 50 записей."),
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
