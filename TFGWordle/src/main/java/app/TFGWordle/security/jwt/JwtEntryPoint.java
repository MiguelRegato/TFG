package app.TFGWordle.security.jwt;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtEntryPoint implements AuthenticationEntryPoint {

    private final static Logger logger = LoggerFactory.getLogger(JwtEntryPoint.class);

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException authException) throws IOException, ServletException {
        logger.error("Fail en el método commence: " + authException.getMessage());

        res.setContentType("application/json"); // Especifica el tipo de contenido como JSON
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // Establece el estado de la respuesta

        // Enviar un objeto JSON con el mensaje de error
        res.getWriter().write("{\"message\": \"No autorizado\"}");
    }

}