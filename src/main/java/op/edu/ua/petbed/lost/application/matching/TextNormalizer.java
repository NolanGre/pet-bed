package op.edu.ua.petbed.lost.application.matching;

import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for normalizing text to improve matching accuracy.
 * Performs: lowercase conversion, punctuation removal, stop word filtering, and synonym replacement.
 */
@NullMarked
@Component
public class TextNormalizer {

    private static final Map<String, String> SYNONYMS = Map.ofEntries(
            // Кольори
            Map.entry("трьохкольоровий", "різнокольоровий"),
            Map.entry("багатокольоровий", "різнокольоровий"),
            Map.entry("багатоколірний", "різнокольоровий"),
            Map.entry("рудий", "помаранчевий"),
            Map.entry("жовтогарячий", "помаранчевий"),
            Map.entry("бурий", "коричневий"),
            Map.entry("шоколадний", "коричневий"),
            Map.entry("каштановий", "коричневий"),
            Map.entry("попелястий", "сірий"),
            Map.entry("димчастий", "сірий"),
            Map.entry("кремовий", "бежевий"),

            // Візерунок
            Map.entry("смугатий", "смугастий"),
            Map.entry("полосатий", "смугастий"),
            Map.entry("тигровий", "смугастий"),
            Map.entry("смужку", "смугастий"),
            Map.entry("плямистий", "плями"),
            Map.entry("плямами", "плями"),
            Map.entry("однотонний", "однокольоровий"),

            Map.entry("дворняга", "безпородний"),
            Map.entry("двірняга", "безпородний"),
            Map.entry("безпорідний", "безпородний"),
            Map.entry("метис", "безпородний"),
            Map.entry("мішаний", "безпородний"),
            Map.entry("напівкровка", "безпородний"),

            Map.entry("великий", "великий"),
            Map.entry("здоровий", "великий"),
            Map.entry("маленький", "малий"),
            Map.entry("невеликий", "малий"),
            Map.entry("середній", "середній"),
            Map.entry("medium", "середній"),
            Map.entry("small", "малий"),
            Map.entry("large", "великий"),

            Map.entry("кошлатий", "пухнастий"),
            Map.entry("кудлатий", "пухнастий"),
            Map.entry("male", "хлопчик"),
            Map.entry("female", "дівчинка")
    );

    private static final Set<String> STOP_WORDS = Set.of(
            "з", "зі", "із", "на", "в", "у", "до", "від", "по", "за", "під", "над", "між", "про",
            "і", "й", "та", "або", "але", "що", "як", "де", "коли", "хоч",
            "він", "вона", "воно", "вони", "його", "її", "їх",
            "є", "був", "була", "було", "були", "дуже", "трохи", "досить", "має", "мав"
    );

    /**
     * Normalizes the input text by:
     * 1. Converting to lowercase
     * 2. Removing punctuation (keeping only Cyrillic, Latin letters, digits, and spaces)
     * 3. Filtering out stop words
     * 4. Replacing synonyms with standardized forms
     *
     * @param input the text to normalize
     * @return normalized text, or empty string if input is null or blank
     */
    public String normalize(String input) {
        if (input.isBlank()) {
            return "";
        }

        String lowercased = input.toLowerCase();

        // Remove punctuation: keep only Cyrillic (including Ukrainian іїє), Latin letters, digits, and spaces
        String withoutPunctuation = lowercased.replaceAll("[^а-яіїєa-z0-9\\s]", " ");

        return Arrays.stream(withoutPunctuation.split("\\s+"))
                .filter(word -> !word.isBlank())
                .filter(word -> !STOP_WORDS.contains(word))
                .map(this::replaceSynonym)
                .collect(Collectors.joining(" "));
    }

    private String replaceSynonym(String word) {
        return SYNONYMS.getOrDefault(word, word);
    }
}
