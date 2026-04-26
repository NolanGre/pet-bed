package op.edu.ua.petbed.telegram.service;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.telegram.callback.CallbackData;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.form.FormEntity;
import op.edu.ua.petbed.telegram.form.FormRepository;
import op.edu.ua.petbed.telegram.form.handler.FormSubmissionHandler;
import op.edu.ua.petbed.telegram.form.scheme.FormInput;
import op.edu.ua.petbed.telegram.form.scheme.FormStep;
import op.edu.ua.petbed.telegram.form.scheme.FormType;
import op.edu.ua.petbed.telegram.response.CallbackListItem;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.KeyboardLayout;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Manages the full lifecycle of user forms: start, input processing, confirmation, and cancellation.
 */
@NullMarked
@Slf4j
@Service
public class FormService {

    private final FormRepository formRepository;
    private final Map<FormType, FormSubmissionHandler> handlersMap;

    public FormService(FormRepository formRepository, List<FormSubmissionHandler> handlers) {
        this.formRepository = formRepository;
        this.handlersMap = Map.copyOf(handlers.stream()
                .collect(Collectors.toMap(FormSubmissionHandler::getFormType, Function.identity())));

        log.debug("Registered form submission handlers: {}", handlersMap.keySet());
    }

    /**
     * Starts a new form session for the user.
     * Deletes any existing form before creating a new one.
     * Returns the first step prompt.
     */
    @Transactional
    public BotApiMethod<?> startCreateForm(FormType type, CallbackId returnCallback, Long internalUserId, Long chatId) {
        formRepository.deleteById(internalUserId);

        FormEntity entity = FormEntity.initiateCreate(internalUserId, chatId, type, returnCallback);
        formRepository.save(entity);

        return ResponseBuilder.sendMessage(chatId)
                .text(entity.nextStep().prompt() + "\n\nℹ️ Для скасування форми /cancel")
                .build();
    }

    /**
     * Starts a new form session with an entity to update.
     */
    @Transactional
    public BotApiMethod<?> startUpdateForm(FormType type, CallbackId returnCallback, Long internalUserId, Long chatId, Long entityId) {
        formRepository.deleteById(internalUserId);

        FormEntity entity = FormEntity.initiateUpdate(internalUserId, chatId, type, returnCallback, entityId);
        formRepository.save(entity);

        return ResponseBuilder.sendMessage(chatId)
                .text(entity.nextStep().prompt() + "\n\nℹ️ Для скасування форми /cancel\nℹ️ Для пропуску кроку /skip")
                .build();
    }

    /**
     * Processes user input for the current step.
     * Validates input — returns error if invalid.
     * Saves answer and returns next prompt or confirmation request if form is complete.
     */
    @Transactional
    public BotApiMethod<?> processInput(FormInput input, Long internalUserId, Long fallbackChatId) {
        var entity = formRepository.findById(internalUserId).orElse(null);

        if (entity == null) {
            return noActiveFormMessage(fallbackChatId);
        }

        FormStep step = entity.nextStep();

        if (!step.validate(input)) {
            return inputValidationMessage(step, entity);
        }

        entity.applyStep(input);
        formRepository.save(entity);

        return nextStepOrCompleteMessage(entity);
    }

    @Transactional
    public BotApiMethod<?> skipStep(Long internalUserId, Long fallbackChatId) {
        var entity = formRepository.findById(internalUserId).orElse(null);
        if (entity == null) {
            return noActiveFormMessage(fallbackChatId);
        }

        FormStep currentStep = entity.nextStep();
        if (!currentStep.canSkip()) {
            return cantSkipMessage(currentStep, entity.getChatId());
        }

        entity.skipStep();
        formRepository.save(entity);
        return nextStepOrCompleteMessage(entity);
    }

    public boolean canSkipCurrentStep(Long internalUserId) {
        return formRepository.findById(internalUserId)
                .filter(e -> !e.isComplete())
                .map(e -> e.nextStep().canSkip())
                .orElse(false);
    }

    /**
     * Confirms the completed form.
     * Converts collected data to DTO, delegates to the appropriate handler, deletes the form.
     */
    @Transactional
    public BotApiMethod<?> confirmForm(Long internalUserId, Long fallbackChatId) {
        var entity = formRepository.findById(internalUserId).orElse(null);

        if (entity == null) {
            return noActiveFormMessage(fallbackChatId);
        }

        if (!entity.isComplete()) {
            return formNotCompleteMessage(entity.getChatId());
        }

        FormSubmissionHandler handler = handlersMap.get(entity.getFormType());
        if (handler == null) {
            throw new PetBedException("No handler for form type: " + entity.getFormType(), PetBedException.ErrorCode.INTERNAL_ERROR);
        }

        var result = handler.handle(entity.toFormData());
        formRepository.delete(entity);

        return result;
    }

    /**
     * Cancels the active form and returns the user to the returnCallback screen.
     */
    @Transactional
    public BotApiMethod<?> cancelForm(Long userInternalId, Long fallbackChatId) {
        var entity = formRepository.findById(userInternalId).orElse(null);

        if (entity == null) {
            return noActiveFormMessage(fallbackChatId);
        }

        Long chatId = entity.getChatId();
        CallbackId returnCallbackId = entity.getReturnCallback();
        Long entityId = entity.getEntityId();

        formRepository.delete(entity);

        return formCancelledMessage(chatId, returnCallbackId, entityId);
    }

    /**
     * Returns true if the user currently has an active form.
     */
    public boolean hasActiveForm(Long userInternalId) {
        return formRepository.existsById(userInternalId);
    }

    /**
     * Returns the active form entity for the user.
     *
     * @throws PetBedException if no active form exists
     */
    public FormEntity getActiveFormOrThrow(Long internalUserId) {
        return formRepository.findById(internalUserId)
                .orElseThrow(() -> new PetBedException("No active form for user", PetBedException.ErrorCode.INTERNAL_ERROR));
    }

    /**
     * Returns the enum keyboard for the current step with pagination.
     * Used by FormEnumListCallbackHandler for pagination navigation.
     */
    public BotApiMethod<?> getEnumKeyboardPage(Long internalUserId, int page, Integer messageId) {
        var entity = formRepository.findById(internalUserId).orElse(null);

        if (entity == null || !entity.nextStep().isChoice()) {
            return noActiveFormMessage(internalUserId);
        }

        FormStep step = entity.nextStep();
        int pageSize = KeyboardLayout.DEFAULT.pageSize();
        List<? extends Enum<?>> allValues = step.enumValues();

        if (allValues == null || allValues.isEmpty()) {
            throw new PetBedException("Enum is empty.", PetBedException.ErrorCode.INTERNAL_ERROR);
        }

        List<CallbackListItem> pagedItems = allValues.stream()
                .skip((long) page * pageSize)
                .limit(pageSize)
                .map(e -> new CallbackListItem(
                        CallbackId.FORM_ENUM_SELECT,
                        (long) e.ordinal(),
                        e.name()
                ))
                .toList();

        Page<CallbackListItem> pageDto = new PageImpl<>(
                pagedItems, PageRequest.of(page, pageSize), allValues.size()
        );

        return ResponseBuilder.editMessage(entity.getChatId(), messageId)
                .text(step.prompt())
                .keyboard(InlineKeyboardBuilder.builder()
                        .paginatedList(pageDto, new CallbackData(CallbackId.FORM_ENUM_LIST.id(), null, page))
                        .build())
                .build();
    }

    private BotApiMethod<?> nextStepOrCompleteMessage(FormEntity entity) {
        if (entity.isComplete()) return formCompleteMessage(entity.getChatId());

        FormStep next = entity.nextStep();
        var builder = ResponseBuilder.sendMessage(entity.getChatId())
                .text(next.prompt());

        if (next.isChoice()) {
            builder.keyboard(InlineKeyboardBuilder.builder()
                    .paginatedList(toPageDto(next), new CallbackData(CallbackId.FORM_ENUM_LIST.id(), null, 0))
                    .build());
        }

        return builder.build();
    }

    private Page<CallbackListItem> toPageDto(FormStep step) {
        int pageSize = KeyboardLayout.DEFAULT.pageSize();
        List<? extends Enum<?>> allValues = step.enumValues();

        if (allValues == null || allValues.isEmpty()) {
            throw new PetBedException("Enum is empty.", PetBedException.ErrorCode.INTERNAL_ERROR);
        }

        List<CallbackListItem> pagedItems = allValues.stream()
                .limit(pageSize)
                .map(e -> new CallbackListItem(
                        CallbackId.FORM_ENUM_SELECT,
                        (long) e.ordinal(),
                        e.name()
                ))
                .toList();

        return new PageImpl<>(
                pagedItems, PageRequest.of(0, pageSize), allValues.size()
        );
    }

    private static BotApiMethod<?> noActiveFormMessage(Long chatId) {
        return ResponseBuilder.sendMessage(chatId)
                .text("📭 У вас немає активних форм")
                .build();
    }

    private static BotApiMethod<?> inputValidationMessage(FormStep step, FormEntity entity) {
        String errorMessage = switch (step.input()) {
            case FormInput.Text _ -> "ℹ️ Очікується саме текст.";
            case FormInput.Photo _ -> "ℹ️ Очікується фото. Надішліть фото.";
            case FormInput.Location _ -> "ℹ️ Очікується геолокація. Надішліть геолокацію.";
            case FormInput.Number _ -> "ℹ️ Очікується ціле число.";
            case FormInput.Choice _ -> "ℹ️ Оберіть варіант з клавіатури вище ☝️";
        };
        return ResponseBuilder.sendMessage(entity.getChatId())
                .text(errorMessage)
                .build();
    }

    private static BotApiMethod<?> formCompleteMessage(Long chatId) {
        return ResponseBuilder.sendMessage(chatId)
                .text("""
                        ✅ Ви завершили заповнення форми!
                        
                        ✏️ Напишіть /submit щоб надіслати
                        🗑️ Або /cancel щоб скасувати
                        """)
                .build();
    }

    private static BotApiMethod<?> formNotCompleteMessage(Long chatId) {
        return ResponseBuilder.sendMessage(chatId)
                .text("⛔ Форма ще не заповнена")
                .build();
    }

    private static BotApiMethod<?> formCancelledMessage(Long chatId, CallbackId returnCallback, @Nullable Long entityId) {
        InlineKeyboardBuilder keyboard = InlineKeyboardBuilder.builder();

        if (entityId == null) {
            keyboard.backButtonTo(returnCallback);
        } else {
            keyboard.backButtonTo(returnCallback, entityId);
        }

        return ResponseBuilder.sendMessage(chatId)
                .text("🗑️ Форму скасовано")
                .keyboard(keyboard.build())
                .build();
    }

    private BotApiMethod<?> cantSkipMessage(FormStep step, Long chatId) {
        return ResponseBuilder.sendMessage(chatId)
                .text("⚠️ Цей крок неможливо пропустити.\n\n" + step.prompt()).build();
    }
}
