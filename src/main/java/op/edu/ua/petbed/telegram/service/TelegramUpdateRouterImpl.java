package op.edu.ua.petbed.telegram.service;

import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.exceptions.WebhookExceptionHandler;
import op.edu.ua.petbed.telegram.TelegramUpdateRouter;
import op.edu.ua.petbed.telegram.auth.TelegramAuthService;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.command.Command;
import op.edu.ua.petbed.telegram.command.CommandContext;
import op.edu.ua.petbed.telegram.command.CommandHandler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class TelegramUpdateRouterImpl implements TelegramUpdateRouter {

    private final TelegramAuthService authService;
    private final Map<Command, CommandHandler> commandHandlerMap;
    private final Map<CallbackId, CallbackHandler> callbackHandlerMap;

    public TelegramUpdateRouterImpl(TelegramAuthService authService, List<CommandHandler> commandHandlers, List<CallbackHandler> callbackHandlers) {
        this.authService = authService;
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
    public BotApiMethod<?> route(Update update) {
        try {
            if (update.hasMessage()) {
                if (isCommand(update)) {
                    return handleCommand(update);
                }
                return handleTextMessage(update);
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

        var from = update.getMessage().getFrom();
        Long userId = from.getId();
        if (command.requiresAuth()) {
            log.debug("Command {} requires auth, authenticating user: {}", command, userId);
            authService.authenticate(userId, from.getUserName());
        }

        CommandContext context = CommandContext.from(update, command);
        CommandHandler handler = commandHandlerMap.get(command);

        if (handler == null) {
            log.warn("No handler for command: {}", command);
            return defaultResponse(context.chatId());
        }

        BotApiMethod<?> response = handler.handle(context);
        log.debug("Command {} handled, response sent to chat: {}", command, context.chatId());
        return response;
    }

    private BotApiMethod<?> handleCallback(Update update) {
        CallbackQueryContext context = CallbackQueryContext.from(update);

        CallbackId callbackId = context.callbackData().callbackIdEnum();

        log.debug("Handling callback id: {}, entityId: {}, offset: {}", callbackId, context.callbackData().entityId(), context.callbackData().offset());

        CallbackHandler handler = callbackHandlerMap.get(callbackId);

        if (handler == null) {
            log.error("No handler for callback id: {}", callbackId);
            return null;
        }

        BotApiMethod<?> response = handler.handle(context);
        log.debug("Callback {} handled, response sent to chat: {}", callbackId, context.chatId());
        return response;
    }

    private BotApiMethod<?> handleTextMessage(Update update) {
        Long chatId = update.getMessage().getChatId();
        return defaultResponse(chatId);
    }

    private SendMessage defaultResponse(Long chatId) {
        return SendMessage.builder()
                .chatId(chatId)
                .text("HandleTextMessage: default response")
                .build();
    }
}