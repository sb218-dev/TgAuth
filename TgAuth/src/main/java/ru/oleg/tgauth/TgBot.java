package ru.oleg.tgauth;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import java.util.Collections;

public class TgBot extends TelegramLongPollingBot {
    private final TgAuthPlugin plugin;

    public TgBot(TgAuthPlugin plugin) {
        super(plugin.getConfig().getString("bot-token"));
        this.plugin = plugin;
    }

    @Override
    public String getBotUsername() { return plugin.getConfig().getString("bot-username"); }

    @Override
    public void onUpdateReceived(Update u) {
        if (u.hasMessage() && u.getMessage().hasText()) {
            long chatId = u.getMessage().getChatId();
            String name = plugin.getDbManager().getPlayerByCode(u.getMessage().getText());
            if (name != null) {
                plugin.getDbManager().linkTelegram(name, chatId);
                sendMsg(chatId, "✅ Аккаунт <b>" + name + "</b> успешно привязан!");
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player p = Bukkit.getPlayerExact(name);
                    if (p != null) plugin.getAuthListener().authenticate(p);
                });
            }
        }
        else if (u.hasCallbackQuery()) {
            String data = u.getCallbackQuery().getData();
            long chatId = u.getCallbackQuery().getMessage().getChatId();
            int msgId = u.getCallbackQuery().getMessage().getMessageId();

            if (data.startsWith("yes_")) {
                // ИСПРАВЛЕНИЕ: Берем всё, что идет ПОСЛЕ "yes_", чтобы не резать ники с "_"
                String name = data.substring(4);

                DeleteMessage dm = new DeleteMessage(String.valueOf(chatId), msgId);
                try { execute(dm); } catch (Exception e) { e.printStackTrace(); }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player p = Bukkit.getPlayerExact(name);
                    if (p != null) {
                        plugin.getAuthListener().authenticate(p);
                    } else {
                        plugin.getLogger().warning("[DEBUG] Игрок " + name + " не найден онлайн!");
                    }
                });
                sendMsg(chatId, "🔓 Вход подтвержден!");
            }
        }
    }

    public void sendAuthRequest(String name, long chatId) {
        SendMessage sm = new SendMessage(String.valueOf(chatId), "⚠️ Вход в аккаунт: <b>" + name + "</b>. Это ты?");
        sm.setParseMode("HTML");
        InlineKeyboardButton b = new InlineKeyboardButton("Да, это я");
        // Здесь тоже будет yes_SaiBot2_18, и substring(4) заберет его целиком
        b.setCallbackData("yes_" + name);
        sm.setReplyMarkup(new InlineKeyboardMarkup(Collections.singletonList(Collections.singletonList(b))));
        try { execute(sm); } catch (Exception e) { e.printStackTrace(); }
    }

    private void sendMsg(long id, String txt) {
        SendMessage sm = new SendMessage(String.valueOf(id), txt);
        sm.setParseMode("HTML");
        try { execute(sm); } catch (Exception e) { e.printStackTrace(); }
    }
}