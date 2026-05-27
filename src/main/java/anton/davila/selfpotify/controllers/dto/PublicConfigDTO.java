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
    /** True si la resolución de carátulas online está activada (COVER_ART_ENABLED). */
    private boolean coverArtEnabled;
    /** Ruta de librería musical auto-detectada del .env (o null si no hay ninguna). */
    private String musicLibraryPath;
    /** Tamaño máximo en bytes del logo que admite POST /api/config/logo. */
    private long logoMaxBytes;
}
