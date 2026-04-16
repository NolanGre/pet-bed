package op.edu.ua.petbed.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CallbackGraphController {

    private final ObjectMapper objectMapper;

    @GetMapping("/callback-graph")
    public ResponseEntity<Void> redirectToGraph() {
        log.info("Code: {}", CallbackId.toMermaidGraph());
        String url = generateMermaidLiveUrl(CallbackId.toMermaidGraph());
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }

    private String generateMermaidLiveUrl(String mermaidGraph) {
        String json = objectMapper.writeValueAsString(
                Map.of("code", mermaidGraph, "mermaid", Map.of("theme", "default")));

        String encoded = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));

        return "https://mermaid.live/edit#base64:" + encoded;
    }
}
