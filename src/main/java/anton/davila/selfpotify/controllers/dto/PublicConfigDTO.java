package anton.davila.selfpotify.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PublicConfigDTO {
    private BrandingDTO branding;
    private boolean setupComplete;
    /** True si hay API key de Last.fm configurada (habilita autocompletar metadatos). */
    private boolean lastfmEnabled;
    /** Ruta de librería musical auto-detectada del .env (o null si no hay ninguna). */
    private String musicLibraryPath;
}
