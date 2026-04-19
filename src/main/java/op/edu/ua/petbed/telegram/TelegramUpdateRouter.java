package op.edu.ua.petbed.telegram;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

@NullMarked
public interface TelegramUpdateRouter {
    @Nullable BotApiMethod<?> route(Update update);
}
