package op.edu.ua.petbed.telegram.command.handler;

import op.edu.ua.petbed.telegram.command.Command;
import op.edu.ua.petbed.telegram.command.CommandContext;
import op.edu.ua.petbed.telegram.command.CommandHandler;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

@NullMarked
@Component
public class StartHandler implements CommandHandler {

    @Override
    public Command getCommand() {
        return Command.START;
    }

    @Override
    public BotApiMethod<?> handle(CommandContext context) {
        return ResponseBuilder.sendMessage(context.chatId())
                .text("""
                        Привіт! 🐾 Це PetBed Bot — твій надійний помічник у світі тварин.
                        
                        Допоможу знайти загубленого улюбленця, прилаштувати тваринку в добрі руки, знайти тимчасовий дім (перетримку) або поділюся актуальними оголошеннями від волонтерів.
                        
                        👇 Що ти можеш зробити прямо зараз (тисни /menu):
                        🔍 Знайшовся/Загубився: Швидко розмістити оголошення.
                        🏠 Адопція: Знайти нову родину або отримати нового друга.
                        ⏱️ Перетримка: Тимчасово прихистити свого улюбленця.
                        📢 Волонтерська стрічка: Важливі оголошення від волонтерів.
                        """)
                .build();
    }
}