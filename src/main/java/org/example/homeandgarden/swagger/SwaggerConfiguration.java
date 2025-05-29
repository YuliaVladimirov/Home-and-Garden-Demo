package org.example.homeandgarden.swagger;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;


@OpenAPIDefinition( info = @Info(title = "Home-and-Garden-Shop REST API", description = "This project is a demo project.", version = "1.0.0", contact = @Contact( name = "Project on GitHub", url = "https://github.com/YuliaVladimirov/Home-and-Garden-Demo.git")))
@SecurityScheme(name = "JWT", type = SecuritySchemeType.HTTP, bearerFormat = "JWT", scheme = "bearer")
public class SwaggerConfiguration {

}
