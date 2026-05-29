package anton.davila.selfpotify.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta al generar un magic link de una playlist: el token de un solo uso y
 * la ruta relativa para canjearlo.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShareLinkResponse {
    private String token;
    private String shareUrl;
}
