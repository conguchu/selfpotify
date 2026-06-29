package anton.davila.selfpotify.controllers;

import anton.davila.selfpotify.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

/**
 * Manejo global de excepciones de la API. Todas las respuestas comparten el mismo
 * formato JSON {@code {status, error, message}}.
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
        return body(HttpStatus.UNAUTHORIZED, "Invalid username or password.");
    }

    /**
     * El límite de subida se supera durante el parseo multipart, antes de entrar al
     * controller, así que la validación del endpoint nunca se ejecuta. Lo traducimos
     * a un 413 con cuerpo JSON legible. El tope que aplica depende del endpoint: el
     * logo tiene el suyo (app.logo.max-file-size); el resto de subidas (audios) usan
     * app.upload.max-file-size. Antes se citaba siempre el del logo (2 MB), lo que
     * confundía al rechazar una canción cuyo límite real es mucho mayor.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSize(MaxUploadSizeExceededException ex,
                                                                   HttpServletRequest request) {
        String uri = request.getRequestURI();
        long mb = (uri != null && uri.contains("/logo"))
                ? appProperties.getLogo().getMaxFileSize().toMegabytes()
                : appProperties.getUpload().getMaxFileSize().toMegabytes();
        log.warn("Subida rechazada por exceder el tamaño máximo ({} MB) en {}", mb, uri);
        return body(HttpStatus.CONTENT_TOO_LARGE,
                "El archivo excede el tamaño máximo permitido (" + mb + " MB).");
    }

    /**
     * Errores HTTP intencionados del código ({@link ResponseStatusException}): se
     * preserva su status y motivo, normalizando el cuerpo al formato común. Tener
     * este handler explícito evita que el handler genérico de abajo los convierta
     * en 500.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatusCode code = ex.getStatusCode();
        String error = (code instanceof HttpStatus hs) ? hs.getReasonPhrase() : "Error";
        String message = ex.getReason() != null ? ex.getReason() : error;
        return ResponseEntity.status(code).body(Map.of(
                "status", code.value(),
                "error", error,
                "message", message));
    }

    /**
     * Body que no cumple las restricciones de validación (@Valid): 400 con el
     * primer mensaje de error por campo en {@code fields}.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fields.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", HttpStatus.BAD_REQUEST.value(),
                "error", HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "message", "Validación fallida",
                "fields", fields));
    }

    /**
     * Red de seguridad para excepciones no previstas: 500 con cuerpo JSON genérico.
     * La traza se registra en el servidor y NO se expone al cliente. Se re-lanzan
     * las excepciones de Spring Security para que el {@code ExceptionTranslationFilter}
     * siga devolviendo 401/403 (no 500).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) throws Exception {
        if (ex instanceof AccessDeniedException || ex instanceof AuthenticationException) {
            throw ex;
        }
        log.error("Error no controlado", ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Error procesando la solicitud.");
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message));
    }
}
