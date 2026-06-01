package anton.davila.selfpotify.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServerConfigDTO {
    private BrandingDTO branding;
    private boolean autoCompleteMetadata;
    private boolean autoCompleteCoverArt;
    private boolean setupComplete;
    private List<String> scanPaths;
    private long scanIntervalSeconds;
    private long lastScanEpochSec;
    // Contexto de ejecución para el panel. El destino de las subidas se elige
    // siempre entre las scanPaths (la subcarpeta selfpotify_added se crea dentro).
    private boolean runningInDocker;
}
