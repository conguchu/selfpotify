package anton.davila.selfpotify.controllers.dto;

public record RescanResultDTO(int added, int recovered, int skipped, int failed) {
}
