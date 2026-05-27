package anton.davila.selfpotify.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConfigUpdateRequest {
    private BrandingDTO branding;
    private Boolean autoCompleteMetadata;
    private Boolean autoCompleteCoverArt;
    private Long scanIntervalSeconds;
}
