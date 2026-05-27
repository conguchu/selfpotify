package anton.davila.selfpotify.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SetupRequest {
    private String appName;
    private List<String> scanPaths;
    private Boolean autoCompleteMetadata;
    private Boolean autoCompleteCoverArt;
    private Long scanIntervalSeconds;
}
