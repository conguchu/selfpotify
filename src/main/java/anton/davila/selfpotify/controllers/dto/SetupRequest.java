package anton.davila.selfpotify.controllers.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SetupRequest {
    // Campos opcionales: las restricciones sólo aplican cuando el valor no es null.
    @Size(max = 64)
    private String appName;
    private List<String> scanPaths;
    private Boolean autoCompleteMetadata;
    private Boolean autoCompleteCoverArt;
    @Min(30)
    @Max(86400)
    private Long scanIntervalSeconds;
}
