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
    private boolean setupComplete;
    private List<String> scanPaths;
    private long scanIntervalSeconds;
    private long lastScanEpochSec;
}
