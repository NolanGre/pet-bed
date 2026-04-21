package op.edu.ua.petbed.telegram.callback;

import op.edu.ua.petbed.common.exceptions.PetBedException;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public enum CallbackId {
    MENU(1, "🏠 Меню", null),

    // PROFILE menu (2xx)
    PROFILE(2, "👤 Профіль", MENU),
    PROFILE_CHANGE_TYPE(21, "Змінити тип профілю", PROFILE),
    PROFILE_CHANGE_TYPE_CONFIRM(211, "✓ Підтвердити зміну", PROFILE_CHANGE_TYPE),

    // MY_PETS menu (3xx)
    MY_PETS(3, "🐾 Мої тварини", MENU),
    ADD_PET(31, "➕ Додати тварину", MY_PETS),
    PET_DETAIL(32, "", MY_PETS),  // Shows as paginated list in MY_PETS
    PET_UPDATE(321, "✏️ Оновити анкету", PET_DETAIL),
    PET_DELETE(322, "🗑️ Видалити анкету", PET_DETAIL),
    PET_DELETE_CONFIRM(3221, "✅ Підтвердити видалення", PET_DELETE),

    // FEED menu (4xx)
    FEED(4, "📋 Стрічка оголошень", MENU),

    FEED_RADIUS(41, "📍 Обрати радіус", FEED),
    FEED_GEOLOCATION(42, "🌍 Обрати геолокацію", FEED),
    FEED_CREATE(43, "➕ Створити оголошення", FEED),

    FEED_MY_POSTS(44, "📋 Мої оголошення", FEED),
    FEED_POST_DETAIL(441, "", FEED_MY_POSTS),
    FEED_POST_DELETE_CONFIRM(4411, "✅ Видалити оголошення", FEED_POST_DETAIL),

    FEED_VIEW(45, "👁️ Переглянути оголошення", FEED),  // When pressed, show first post by editing message
    FEED_VIEW_NEXT(451, "➡️ Наступне", FEED_VIEW),  // Send new message each time

    // LOST menu (5xx)
    LOST(5, "🔍 Пошук тварин", MENU),

    LOST_START(51, "🔎 Почати пошук", LOST),
    LOST_SELECT_PET(511, "🐾 Обрати тварину", LOST_START),
    LOST_ADD_PET(5111, "➕ Додати нову тварину", LOST_SELECT_PET),

    LOST_FOUND(52, "🐕 Я знайшов тварину", LOST),
    LOST_FOUND_MATCHES(521, "🔗 Потенційні збіги", LOST_FOUND), // Shows after filling out the form

    LOST_ACTIVE(53, "📋 Мої пошуки", LOST),
    LOST_ACTIVE_DETAIL(531, "", LOST_ACTIVE),

    LOST_FINISH(5311, "🏁 Завершити пошук", LOST_ACTIVE_DETAIL),
    LOST_FINISH_CONFIRM(53111, "✅ Підтвердити", LOST_FINISH),

    LOST_RECOMMENDATIONS(5312, "💡 Рекомендації", LOST_ACTIVE_DETAIL),       // Show first recommendation by editing message
    LOST_RECOMMENDATION_NEXT(53121, "➡️ Наступна", LOST_RECOMMENDATIONS),   // Each new recom. send as new message
    LOST_CLAIM_PET(53122, "🐾 Це моя тварина", LOST_RECOMMENDATIONS),
    LOST_CLAIM_PET_CONFIRM(531221, "✅ Підтвердити", LOST_CLAIM_PET),

    // ADOPTION menu (6xx)
    ADOPTION(6, "🏠 Адопція", MENU),

    ADOPTION_GIVE(61, "🤝 Віддати тварину", ADOPTION),
    ADOPTION_SELECT_PET(611, "🐾 Обрати тварину", ADOPTION_GIVE),
    ADOPTION_PET_DETAIL(6111, "", ADOPTION_SELECT_PET),      // Shows as paginated list in ADOPTION_GIVE

    ADOPTION_ADD_PET(6112, "➕ Додати нову тварину", ADOPTION_SELECT_PET),   // Start form

    ADOPTION_ADD_COMMENT(612, "💬 Додати коментар", ADOPTION_GIVE),   // Start form
    ADOPTION_TRY_CONFIRM(613, "✅ Підтвердити", ADOPTION_GIVE),   // If it can be done
    ADOPTION_FINAL_CONFIRM(6131, "🏁 Підтвердити", ADOPTION_TRY_CONFIRM),     // Submit form

    ADOPTION_CLEAR(614, "🗑️ Очистити", ADOPTION_GIVE),
    ADOPTION_CLEAR_CONFIRM(6141, "✅ Підтвердити", ADOPTION_CLEAR),

    ADOPTION_GET(62, "🐾 Отримати тварину", ADOPTION),   // Shows by editing existing message
    ADOPTION_GET_NEXT(621, "➡️ Наступна", ADOPTION_GET),
    ADOPTION_RESPONSE_CREATE(622, "✉️ Відгукнутись", ADOPTION_GET),  // Start form for comment

    ADOPTION_MY_RESPONSES(63, "✉️ Мої відгуки", ADOPTION),
    ADOPTION_MY_RESPONSE_DETAIL(631, "", ADOPTION_MY_RESPONSES),  // Shows as paginated list in ADOPTION_MY_RESPONSES
    ADOPTION_MY_RESPONSE_DETAIL_CANCEL(6311, "❌ Відмінити", ADOPTION_MY_RESPONSE_DETAIL),
    ADOPTION_MY_RESPONSE_DETAIL_CANCEL_CONFIRM(63111, "✅ Підтвердити", ADOPTION_MY_RESPONSE_DETAIL_CANCEL),

    ADOPTION_MY_POSTS(64, "📋 Мої оголошення", ADOPTION),
    ADOPTION_POST_DETAIL(641, "", ADOPTION_MY_POSTS),      // Shows as paginated list in ADOPTION_MY_POSTS
    ADOPTION_RESPONSE_DETAIL(6411, "", ADOPTION_POST_DETAIL),     // Shows as paginated list in ADOPTION_POST_DETAIL

    ADOPTION_RESPONSE_DETAIL_TRY_CONFIRM(64111, "✅ Підтвердити", ADOPTION_RESPONSE_DETAIL),
    ADOPTION_RESPONSE_DETAIL_CONFIRM(641111, "✅ Підтвердити", ADOPTION_RESPONSE_DETAIL_TRY_CONFIRM),

    ADOPTION_RESPONSE_DETAIL_TRY_CANCEL(64112, "❌ Відхилити", ADOPTION_RESPONSE_DETAIL),
    ADOPTION_RESPONSE_DETAIL_CANCEL(641121, "❌ Відхилити", ADOPTION_RESPONSE_DETAIL_TRY_CANCEL),

    ADOPTION_POST_TRY_DELETE(6412, "🗑️ Видалити оголошення", ADOPTION_POST_DETAIL),
    ADOPTION_POST_DELETE(64121, "✅ Підтвердити", ADOPTION_POST_TRY_DELETE),

    // FOSTERING menu (7xx)
    FOSTERING(7, "⏳ Перетримка", MENU),

    FOSTERING_GIVE(71, "🤝 Почати перетримку", FOSTERING),
    FOSTERING_SELECT_PET(711, "🐾 Обрати тварину", FOSTERING_GIVE),
    FOSTERING_PET_DETAIL(7111, "", FOSTERING_SELECT_PET),      // Shows as paginated list in FOSTERING_GIVE

    FOSTERING_ADD_PET(7112, "➕ Додати нову тварину", FOSTERING_SELECT_PET),   // Start form

    FOSTERING_ADD_COMMENT(712, "💬 Додати коментар", FOSTERING_GIVE),   // Start form
    FOSTERING_ADD_DURATION(713, "⏳ Обрати тривалість", FOSTERING_GIVE),   // Start form

    FOSTERING_TRY_CONFIRM(714, "✅ Підтвердити", FOSTERING_GIVE),   // If it can be done
    FOSTERING_FINAL_CONFIRM(7141, "🏁 Підтвердити", FOSTERING_TRY_CONFIRM),     // Submit form

    FOSTERING_CLEAR(715, "🗑️ Очистити", FOSTERING_GIVE),
    FOSTERING_CLEAR_CONFIRM(7151, "✅ Підтвердити", FOSTERING_CLEAR),

    FOSTERING_GET(72, "🐾 Перетримати тварину", FOSTERING),   // Shows by editing existing message
    FOSTERING_GET_NEXT(721, "➡️ Наступна", FOSTERING_GET),
    FOSTERING_RESPONSE_CREATE(722, "✉️ Відгукнутись", FOSTERING_GET),  // Start form for comment

    FOSTERING_MY_RESPONSES(73, "✉️ Мої відгуки", FOSTERING),
    FOSTERING_MY_RESPONSE_DETAIL(731, "", FOSTERING_MY_RESPONSES),  // Shows as paginated list in FOSTERING_MY_RESPONSES
    FOSTERING_MY_RESPONSE_DETAIL_CANCEL(7311, "❌ Відмінити", FOSTERING_MY_RESPONSE_DETAIL),
    FOSTERING_MY_RESPONSE_DETAIL_CANCEL_CONFIRM(73111, "✅ Підтвердити", FOSTERING_MY_RESPONSE_DETAIL_CANCEL),

    FOSTERING_MY_POSTS(74, "📋 Мої оголошення", FOSTERING),
    FOSTERING_POST_DETAIL(741, "", FOSTERING_MY_POSTS),      // Shows as paginated list in FOSTERING_MY_POSTS
    FOSTERING_RESPONSE_DETAIL(7411, "", FOSTERING_POST_DETAIL),     // Shows as paginated list in FOSTERING_POST_DETAIL

    FOSTERING_RESPONSE_DETAIL_TRY_CONFIRM(74111, "✅ Підтвердити", FOSTERING_RESPONSE_DETAIL),
    FOSTERING_RESPONSE_DETAIL_CONFIRM(741111, "✅ Підтвердити", FOSTERING_RESPONSE_DETAIL_TRY_CONFIRM),

    FOSTERING_RESPONSE_DETAIL_TRY_CANCEL(74112, "❌ Відхилити", FOSTERING_RESPONSE_DETAIL),
    FOSTERING_RESPONSE_DETAIL_CANCEL(741121, "❌ Відхилити", FOSTERING_RESPONSE_DETAIL_TRY_CANCEL),

    FOSTERING_POST_TRY_DELETE(7412, "🗑️ Видалити оголошення", FOSTERING_POST_DETAIL),
    FOSTERING_POST_DELETE(74121, "✅ Підтвердити", FOSTERING_POST_TRY_DELETE),

    // PAGINATION STUB
    PAGINATION_PAGE_INDICATOR(900, "", null);

    public static final String BACK_BUTTON_LABEL = "⬅️ Повернутись";

    private final int id;
    private final String label;
    private final @Nullable CallbackId parent;

    CallbackId(int id, String label, @Nullable CallbackId parent) {
        this.id = id;
        this.label = label;
        this.parent = parent;
    }

    public List<CallbackId> children() {
        return Arrays.stream(values())
                .filter(e -> e.parent == this)
                .toList();
    }

    public int id() {
        return id;
    }

    public String label() {
        return label;
    }

    public @Nullable CallbackId parent() {
        return parent;
    }

    public boolean hasParent() {
        return parent != null;
    }

    public String backButtonLabel() {
        return BACK_BUTTON_LABEL;
    }

    public static CallbackId fromId(int id) {
        return Arrays.stream(values())
                .filter(e -> e.id == id)
                .findFirst()
                .orElseThrow(() -> new PetBedException("Unknown callback id: " + id, PetBedException.ErrorCode.INVALID_CALLBACK));
    }

    public static CallbackId fromId(String id) {
        if (id == null || id.isBlank()) {
            throw new PetBedException("Callback id is required", PetBedException.ErrorCode.INVALID_CALLBACK);
        }
        try {
            return fromId(Integer.parseInt(id));
        } catch (NumberFormatException e) {
            throw new PetBedException("Invalid callback id format: " + id, PetBedException.ErrorCode.INVALID_CALLBACK);
        }
    }

    public static String toMermaidGraph() {
        StringBuilder sb = new StringBuilder();
        sb.append("graph TD\n");
        for (CallbackId e : values()) {
            String nodeLabel = e.label().replace("\"", "\\\"");
            sb.append(e.name()).append("[\"").append(nodeLabel).append("\"]\n");
        }
        for (CallbackId e : values()) {
            if (e.parent != null) {
                sb.append(e.parent.name()).append(" --> ").append(e.name()).append("\n");
            }
        }
        return sb.toString();
    }
}