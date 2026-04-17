package op.edu.ua.petbed.telegram.callback;

import op.edu.ua.petbed.common.exceptions.PetBedException;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public enum CallbackId {
    MENU(100, "Меню", null),

    // MENU children
    PROFILE(110, "Профіль", MENU),
    MY_PETS(120, "Мої тварини", MENU),
    FEED(130, "Стрічка оголошень", MENU),
    SEARCH(140, "Пошук тварин", MENU),
    FOSTER(180, "Перетримка", MENU),
    ADOPTION(160, "Адопція", MENU),

    // PROFILE children
    PROFILE_CHANGE_TYPE_CONFIRM(111, "Підтвердження зміни типу", PROFILE),

    // MY_PETS children
    ADD_PET(121, "Додати тварину", MY_PETS),
    PET_LIST(122, "Список тварин", MY_PETS),

    // PET_LIST -> PET_DETAIL chain
    PET_DETAIL(123, "", PET_LIST),    // List element
    PET_UPDATE(124, "Оновити анкету", PET_DETAIL),
    PET_DELETE(125, "Видалити анкету", PET_DETAIL),
    PET_DELETE_CONFIRM(126, "Підтвердити видалення", PET_DELETE),

    // FEED children
    FEED_RADIUS(131, "Обрати радіус", FEED),
    FEED_GEOLOCATION(132, "Обрати геолокацію", FEED),
    FEED_CREATE(133, "Створити оголошення", FEED),
    FEED_MY_POSTS(134, "Мої оголошення", FEED),
    FEED_VIEW(135, "Перегляд оголошення", FEED),

    // FEED_MY_POSTS -> FEED_POST_DETAIL chain
    FEED_POST_DETAIL(136, "Оголошення", FEED_MY_POSTS),
    FEED_POST_DELETE_CONFIRM(137, "Видалити оголошення", FEED_POST_DETAIL),

    // SEARCH children
    SEARCH_START(141, "Почати пошук", SEARCH),
    SEARCH_FOUND(142, "Я знайшов тварину", SEARCH),
    SEARCH_ACTIVE(143, "Активні пошуки", SEARCH),

    // SEARCH_START -> SEARCH_SELECT_PET chain
    SEARCH_SELECT_PET(144, "Обрати тварину", SEARCH_START),
    SEARCH_ADD_PET(146, "Додати нову тварину", SEARCH_SELECT_PET),
    SEARCH_FINAL_CONFIRM(145, "Підтвердження", SEARCH_START),

    // SEARCH_FOUND -> SEARCH_FOUND_MATCHES chain
    SEARCH_FOUND_MATCHES(147, "Потенційні збіги", SEARCH_FOUND),

    // SEARCH_ACTIVE -> SEARCH_ACTIVE_DETAIL chain
    SEARCH_ACTIVE_DETAIL(148, "Тварина у пошуку", SEARCH_ACTIVE),
    SEARCH_RECOMMENDATIONS(149, "Рекомендації", SEARCH_ACTIVE_DETAIL),
    SEARCH_FINISH_CONFIRM(150, "Завершити пошук", SEARCH_ACTIVE_DETAIL),

    // SEARCH_RECOMMENDATIONS -> SEARCH_CLAIM_PET chain
    SEARCH_CLAIM_PET(151, "Це моя тварина", SEARCH_RECOMMENDATIONS),

    // ADOPTION children
    ADOPTION_GIVE(161, "Віддати тварину", ADOPTION),
    ADOPTION_GET(162, "Отримати тварину", ADOPTION),
    ADOPTION_MY_RESPONSES(163, "Мої відгуки", ADOPTION),
    ADOPTION_MY_POSTS(164, "Мої оголошення", ADOPTION),

    // ADOPTION_GIVE -> chain
    ADOPTION_SELECT_PET(165, "Обрати тварину", ADOPTION_GIVE),
    ADOPTION_ADD_PET(169, "Додати нову тварину", ADOPTION_SELECT_PET),
    ADOPTION_ADD_COMMENT(166, "Додати коментар", ADOPTION_GIVE),
    ADOPTION_FINAL_CONFIRM(167, "Підтвердити", ADOPTION_GIVE),
    ADOPTION_CLEAR(168, "Очистити", ADOPTION_GIVE),

    // ADOPTION_GET -> ADOPTION_RESPONSE_CREATE chain
    ADOPTION_RESPONSE_CREATE(170, "Створити відгук", ADOPTION_GET),

    // ADOPTION_MY_RESPONSES -> ADOPTION_MY_RESPONSE_DETAIL chain
    ADOPTION_MY_RESPONSE_DETAIL(171, "Мій відгук", ADOPTION_MY_RESPONSES),

    // ADOPTION_MY_POSTS -> ADOPTION_POST_DETAIL chain
    ADOPTION_POST_DETAIL(172, "Оголошення", ADOPTION_MY_POSTS),
    ADOPTION_RESPONSE_DETAIL(173, "Відгук", ADOPTION_POST_DETAIL),
    ADOPTION_POST_DELETE_CONFIRM(174, "Видалити оголошення", ADOPTION_POST_DETAIL),

    // FOSTER children (mirror ADOPTION)
    FOSTER_GIVE(181, "Віддати тварину", FOSTER),
    FOSTER_GET(182, "Отримати тварину", FOSTER),
    FOSTER_MY_RESPONSES(183, "Мої відгуки", FOSTER),
    FOSTER_MY_POSTS(184, "Мої оголошення", FOSTER),

    // PAGINATION STUBS
    PAGINATION_PAGE_INDICATOR(300, "", null);

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
