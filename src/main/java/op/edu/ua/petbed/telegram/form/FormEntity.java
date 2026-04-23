package op.edu.ua.petbed.telegram.form;

import jakarta.persistence.*;
import lombok.*;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.AbstractAuditableEntity;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.form.scheme.FormInput;
import op.edu.ua.petbed.telegram.form.scheme.FormStep;
import op.edu.ua.petbed.telegram.form.scheme.FormType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Form model: persists user answers and provides form progression API.
 * Schema defines what to ask — model tracks what was answered.
 */
@NullMarked
@ToString
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, onConstructor_ = @SuppressWarnings("NullAway"))
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "user_form_states")
public class FormEntity extends AbstractAuditableEntity {

    @Id
    @Column(name = "telegram_id", nullable = false)
    private Long telegramId;

    @Column(name = "chat_id", nullable = false, unique = true)
    private Long chatId;

    @Column(name = "form_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private FormType formType;

    @Column(name = "return_callback", nullable = false)
    @Enumerated(EnumType.STRING)
    private CallbackId returnCallback;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_steps", columnDefinition = "jsonb")
    @Getter(AccessLevel.PRIVATE)
    private Map<Integer, String> rawSteps = new LinkedHashMap<>();

    /**
     * Creates a new empty form session for the given user.
     */
    public static FormEntity initiate(Long telegramId, Long chatId, FormType formType, CallbackId returnCallback) {
        FormEntity e = new FormEntity();
        e.telegramId = telegramId;
        e.chatId = chatId;
        e.returnCallback = returnCallback;
        e.formType = formType;
        return e;
    }

    /**
     * Returns the next unfilled step from the schema.
     */
    public FormStep nextStep() {
        if (isComplete()) {
            throw new PetBedException("Form already completed", PetBedException.ErrorCode.INTERNAL_ERROR);
        }
        return formType.steps().get(rawSteps.size());
    }

    /**
     * Whether all steps of the schema have been filled.
     */
    public boolean isComplete() {
        return rawSteps.size() >= formType.steps().size();
    }

    /**
     * Persists the user's answer for the current step.
     */
    public void applyStep(FormInput input) {
        rawSteps.put(rawSteps.size(), serialize(input));
    }

    private static String serialize(FormInput input) {
        return switch (input) {
            case FormInput.Text(String value) -> value;
            case FormInput.Photo(String fileId) -> fileId;
            case FormInput.Location(double lat, double lon) -> lat + "," + lon;
        };
    }

    @Override
    public final boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        FormEntity that = (FormEntity) o;
        return Objects.equals(getTelegramId(), that.getTelegramId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
