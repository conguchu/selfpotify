package anton.davila.selfpotify.controllers;

import anton.davila.selfpotify.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

/**
 * Manejo global de excepciones de la API.
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final AppProperties appProperties;

    /**
     * Credenciales incorrectas en POST /api/auth/login: devuelve 401 en vez
     * de dejar que Spring lance un 500 no controlado.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "status", HttpStatus.UNAUTHORIZED.value(),
                        "error", HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                        "message", "Invalid username or password."));
    }

    /**
     * El límite de subida (spring.servlet.multipart.max-file-size) se supera
     * durante el parseo multipart, antes de entrar al controller, así que la
     * validación del propio endpoint nunca se ejecuta. Lo traducimos a un
     * 413 con cuerpo JSON ({@code {status, error, message}}) que el cliente puede
     * leer, en vez de dejar que se resuelva con un error genérico.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        long mb = appProperties.getLogo().getMaxFileSize().toMegabytes();
        String message = "El archivo excede el tamaño máximo permitido (" + mb + " MB).";
        log.warn("Subida rechazada por exceder el tamaño máximo ({} MB)", mb);
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
                .body(Map.of(
                        "status", HttpStatus.CONTENT_TOO_LARGE.value(),
                        "error", HttpStatus.CONTENT_TOO_LARGE.getReasonPhrase(),
                        "message", message));
    }
}
