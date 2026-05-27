package anton.davila.selfpotify;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerGlobalConfig {

    private Branding branding = new Branding();
    private Features features = new Features();
    private Scan scan = new Scan();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Branding {
        private String appName = "selfpotify";
        private String logoUrl;
        private Map<String, String> colors = defaultColors();

        public static Map<String, String> defaultColors() {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("--color-bg", "#0a0a0a");
            m.put("--color-bg-elevated", "#141414");
            m.put("--color-bg-card", "#1a1a1a");
            m.put("--color-bg-hover", "#232323");
            m.put("--color-border", "#262626");
            m.put("--color-text", "#f5f5f5");
            m.put("--color-text-muted", "#a3a3a3");
            m.put("--color-text-subtle", "#6b6b6b");
            m.put("--color-accent", "#b91c1c");
            m.put("--color-accent-hover", "#dc2626");
            m.put("--color-accent-active", "#991b1b");
            m.put("--color-accent-soft", "#450a0a");
            m.put("--color-danger", "#ef4444");
            m.put("--color-success", "#16a34a");
            return m;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Features {
        private boolean autoCompleteMetadata = false;
        private boolean autoCompleteCoverArt = false;
        private boolean setupComplete = false;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Scan {
        private List<String> paths = new ArrayList<>();
        private long intervalSeconds = 3600;
        private long lastRunEpochSec = 0;
    }
}
