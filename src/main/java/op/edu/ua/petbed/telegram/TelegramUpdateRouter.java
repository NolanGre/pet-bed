package op.edu.ua.petbed.telegram;

import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface TelegramUpdateRouter {
    BotApiMethod<?> route(Update update);
}
