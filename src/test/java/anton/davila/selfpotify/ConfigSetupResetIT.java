package anton.davila.selfpotify;

import com.fasterxml.jackson.databind.ObjectMapper;
import anton.davila.selfpotify.config.ConfigService;
import anton.davila.selfpotify.controllers.dto.SetupRequest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import jakarta.annotation.PostConstruct;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConfigSetupResetIT {

    @TempDir
    static Path tmp;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("app.config.path", () -> tmp.resolve("config.yml").toString());
    }

    @Autowired WebApplicationContext context;
    @Autowired ConfigService configService;
    final ObjectMapper json = new ObjectMapper();

    MockMvc mvc;

    @PostConstruct
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test @Order(1)
    void publicEndpointBeforeSetup_returnsSetupCompleteFalse() throws Exception {
        mvc.perform(get("/api/config/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setupComplete").value(false))
                .andExpect(jsonPath("$.branding.appName").exists());
    }

    @Test @Order(2)
    @WithMockUser(username = "alice", roles = {"USER"})
    void setupAsNonAdmin_isForbidden() throws Exception {
        mvc.perform(post("/api/config/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test @Order(3)
    @WithMockUser(roles = "ADMIN")
    void setupWithInvalidScanPath_returns400() throws Exception {
        SetupRequest bad = new SetupRequest();
        bad.setScanPaths(List.of("/this/path/does/not/exist/xyz"));
        mvc.perform(post("/api/config/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    @Test @Order(4)
    @WithMockUser(roles = "ADMIN")
    void setupWithInvalidInterval_returns400() throws Exception {
        SetupRequest bad = new SetupRequest();
        bad.setScanIntervalSeconds(1L);
        mvc.perform(post("/api/config/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    @Test @Order(5)
    @WithMockUser(roles = "ADMIN")
    void setupSucceedsAndMarksComplete() throws Exception {
        Path music = Files.createDirectory(tmp.resolve("music"));
        SetupRequest req = new SetupRequest();
        req.setAppName("MiSelfpotify");
        req.setAutoCompleteMetadata(true);
        req.setScanIntervalSeconds(300L);
        req.setScanPaths(List.of(music.toString()));

        mvc.perform(post("/api/config/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setupComplete").value(true))
                .andExpect(jsonPath("$.branding.appName").value("MiSelfpotify"))
                .andExpect(jsonPath("$.autoCompleteMetadata").value(true))
                .andExpect(jsonPath("$.scanIntervalSeconds").value(300))
                .andExpect(jsonPath("$.scanPaths[0]").value(music.toAbsolutePath().normalize().toString()));
    }

    @Test @Order(6)
    @WithMockUser(roles = "ADMIN")
    void secondSetupCall_returns409() throws Exception {
        mvc.perform(post("/api/config/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict());
    }

    @Test @Order(7)
    void publicEndpointAfterSetup_returnsSetupCompleteTrue() throws Exception {
        mvc.perform(get("/api/config/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setupComplete").value(true))
                .andExpect(jsonPath("$.branding.appName").value("MiSelfpotify"));
    }

    @Test @Order(8)
    @WithMockUser(username = "alice", roles = {"USER"})
    void resetAsNonAdmin_isForbidden() throws Exception {
        mvc.perform(post("/api/config/reset"))
                .andExpect(status().isForbidden());
    }

    @Test @Order(9)
    @WithMockUser(roles = "ADMIN")
    void resetClearsConfigAndSetupFlag() throws Exception {
        mvc.perform(post("/api/config/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        // config en memoria vuelve a estar en blanco
        org.junit.jupiter.api.Assertions.assertFalse(
                configService.getConfig().getFeatures().isSetupComplete());
        org.junit.jupiter.api.Assertions.assertTrue(
                configService.getConfig().getScan().getPaths().isEmpty());

        // y vuelve a permitirse setup
        mvc.perform(get("/api/config/public"))
                .andExpect(jsonPath("$.setupComplete").value(false));
    }
}
