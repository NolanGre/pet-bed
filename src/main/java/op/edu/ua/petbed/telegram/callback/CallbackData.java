package op.edu.ua.petbed.telegram.callback;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

/**
 * Record representing parsed callback_data from inline keyboard button.
 * <p>
 * Telegram buttons send JSON in callback_data field. This record parses it:
 * <b>JSON format:</b>
 * <pre>
 * {"a":"ACTION_NAME", "p":"optional_payload", "o":10}
 * </pre>
 *
 * <b>Fields:</b>
 * <ul>
 *     <li>{@code a} - action name (becomes enum)</li>
 *     <li>{@code p} - optional payload string</li>
 *     <li>{@code o} - optional offset integer</li>
 * </ul>
 *
 * @see CallbackAction
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CallbackData(
        /* Action name (maps to CallbackAction enum). */
        @JsonProperty("a") String action,

        /* Optional payload string (e.g., item ID). */
        @JsonProperty("p") @Nullable String payload,

        /* Optional offset for pagination. */
        @JsonProperty("o") @Nullable Integer offset
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Parses JSON string to CallbackData.
     *
     * @param json raw callback_data from Telegram button
     * @return parsed CallbackData with action as string
     */
    public static CallbackData from(@Nullable String json) {
        if (StringUtils.isBlank(json)) {
            return new CallbackData("unknown", null, null);
        }
        try {
            JsonNode node = MAPPER.readTree(json);
            String action = node.has("a") ? node.get("a").asText() : "unknown";
            String payload = node.has("p") ? node.get("p").asText() : null;
            Integer offset = node.has("o") ? node.get("o").asInt() : null;
            return new CallbackData(action, payload, offset);
        } catch (Exception e) {
            return new CallbackData("unknown", null, null);
        }
    }

    /**
     * Converts to JSON string for button callback_data.
     *
     * @return JSON string
     */
    public String toJson() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (Exception e) {
            return "{\"a\":\"error\"}";
        }
    }
}