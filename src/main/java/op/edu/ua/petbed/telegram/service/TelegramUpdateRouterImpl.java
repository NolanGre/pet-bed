package op.edu.ua.petbed.telegram.service;

import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.exceptions.WebhookExceptionHandler;
import op.edu.ua.petbed.telegram.TelegramUpdateRouter;
import op.edu.ua.petbed.telegram.auth.TelegramAuthService;
import op.edu.ua.petbed.telegram.auth.UserAuthContext;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.command.Command;
import op.edu.ua.petbed.telegram.command.CommandContext;
import op.edu.ua.petbed.telegram.command.CommandHandler;
import op.edu.ua.petbed.telegram.form.scheme.FormInput;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@NullMarked
@Component
@Slf4j
public class TelegramUpdateRouterImpl implements TelegramUpdateRouter {

    private final TelegramAuthService authService;
    private final FormService formService;
    private final TelegramClient telegramClient;

    private final Map<Command, CommandHandler> commandHandlerMap;
    private final Map<CallbackId, CallbackHandler> callbackHandlerMap;

    public TelegramUpdateRouterImpl(TelegramAuthService authService, List<CommandHandler> commandHandlers, List<CallbackHandler> callbackHandlers, FormService formService, TelegramClient telegramClient) {
        this.authService = authService;
        this.formService = formService;
        this.telegramClient = telegramClient;

        this.commandHandlerMap = Map.copyOf(commandHandlers.stream()
                .collect(Collectors.toMap(CommandHandler::getCommand, Function.identity())));
        this.callbackHandlerMap = Map.copyOf(callbackHandlers.stream()
                .collect(Collectors.toMap(CallbackHandler::getCallbackId, Function.identity())));

        log.debug("Registered command handlers: {}", commandHandlerMap.keySet());
        log.debug("Registered callback handlers: {}", callbackHandlerMap.keySet());

        for (Command c : Command.values()) {
            if (c != Command.DEFAULT && !commandHandlerMap.containsKey(c)) {
                log.error("No handler registered for command: {}", c);
            }
        }
    }

    @Override
    public @Nullable BotApiMethod<?> route(Update update) {
        try {
            if (update.hasMessage()) {
                if (isCommand(update)) {
                    return handleCommand(update);
                }
                return handleMessage(update);
            }
            if (update.hasCallbackQuery()) {
                return handleCallback(update);
            }
            throw new PetBedException("Unsupported update type", PetBedException.ErrorCode.UNSUPPORTED_UPDATE);
        } catch (Exception e) {
            return WebhookExceptionHandler.handle(update, e);
        }
    }

    private boolean isCommand(Update update) {
        List<MessageEntity> entities = update.getMessage().getEntities();
        return entities != null && entities.stream()
                .anyMatch(e -> "bot_command".equals(e.getType()));
    }

    private BotApiMethod<?> handleCommand(Update update) {
        String text = update.getMessage().getText();
        Command command = Command.fromLabel(text);
        log.debug("Handling command: {} with text: {}", command, text);

        var authContext = authUser(update);
        CommandContext context = CommandContext.from(update, command, authContext);
        CommandHandler handler = commandHandlerMap.get(command);

        if (handler == null) {
            log.warn("No handler for command: {}", command);
            return defaultResponse(context.chatId());
        }

        PartialBotApiMethod<?> response = handler.handle(context);
        log.debug("Command {} handled, response sent to chat: {}", command, context.chatId());
        return deliver(response, null);
    }

    private BotApiMethod<?> handleCallback(Update update) {
        var authContext = authUser(update);
        CallbackQueryContext context = CallbackQueryContext.from(update, authContext);

        CallbackId callbackId = context.callbackData().callbackIdEnum();

        if (callbackId == CallbackId.PAGINATION_PAGE_INDICATOR) {
            return AnswerCallbackQuery.builder().callbackQueryId(context.callbackQuery().getId()).build();
        }

        log.debug("Handling callback id: {}, entityId: {}, offset: {}", callbackId, context.callbackData().entityId(), context.callbackData().offset());

        CallbackHandler handler = callbackHandlerMap.get(callbackId);

        if (handler == null) {
            log.error("No handler for callback id: {}", callbackId);
            return defaultCallbackResponse(context.callbackQuery().getId());
        }

        PartialBotApiMethod<?> response = handler.handle(context);
        log.debug("Callback {} handled, response sent to chat: {}", callbackId, context.chatId());
        return deliver(response, context.callbackQuery().getId());
    }

    // Hande all type of message: text, photo, location etc.
    private BotApiMethod<?> handleMessage(Update update) {
        var auth = authUser(update);
        Message message = update.getMessage();
        Long chatId = message.getChatId();

        if (formService.hasActiveForm(auth.userInternalId())) {
            FormInput input = FormInput.from(message);
            log.debug("Processing form input for userTelegramId: {}, type: {}", auth.userTelegramId(), input.getClass().getSimpleName());
            return formService.processInput(input, auth.userInternalId(), chatId);
        }

        log.debug("No active form for userTelegramId: {}, returning default response", auth.userTelegramId());
        return defaultMessageResponse(chatId);
    }

    private UserAuthContext authUser(Update update) {
        var from = update.hasCallbackQuery()
                ? update.getCallbackQuery().getFrom()
                : update.getMessage().getFrom();
        return authService.authenticate(from.getId(), from.getUserName());
    }

    private SendMessage defaultResponse(Long chatId) {
        return SendMessage.builder()
                .chatId(chatId)
                .text("⚠️ Тимчасово не доступно.")
                .build();
    }

    private AnswerCallbackQuery defaultCallbackResponse(String callbackQueryId) {
        return AnswerCallbackQuery.builder()
                .callbackQueryId(callbackQueryId)
                .text("⚠️ Тимчасово не доступно.")
                .showAlert(true)
                .build();
    }

    private SendMessage defaultMessageResponse(Long chatId) {
        return SendMessage.builder()
                .chatId(chatId)
                .text("Не зрозумів 🤔")
                .build();
    }

    private BotApiMethod<?> deliver(PartialBotApiMethod<?> response, @Nullable String callbackQueryId) {
        if (response instanceof BotApiMethod<?> method) {
            return method; // SendMessage, EditMessageText, SendLocation...
        }

        try {
            switch (response) {
                case SendPhoto p -> telegramClient.execute(p);
                case SendDocument d -> telegramClient.execute(d);
                case SendVideo v -> telegramClient.execute(v);
                case EditMessageMedia e -> telegramClient.execute(e);
                default -> log.error("Unsupported media type for deliver: {}", response.getClass().getSimpleName());
            }
        } catch (TelegramApiException e) {
            log.error("Failed to deliver media response", e);
        }

        // Stub for webhook response
        return AnswerCallbackQuery.builder()
                .callbackQueryId(callbackQueryId != null ? callbackQueryId : "")
                .build();
    }
}