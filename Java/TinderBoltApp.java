package com.javarush.telegram;

import java.util.ArrayList;
import java.util.List;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class TinderBoltApp extends MultiSessionTelegramBot {
    public static final String TELEGRAM_BOT_NAME = System.getenv("TELEGRAM_BOT_NAME"); 
    public static final String TELEGRAM_BOT_TOKEN = System.getenv("TELEGRAM_BOT_TOKEN");
    public static final String OPEN_AI_TOKEN = System.getenv("OPEN_AI_TOKEN"); 
   
    public DialogMode mode = DialogMode.MAIN;
    private List<String> chat;
    private UserInfo myInfo;
    private int questionNumber;
    private UserInfo personInfo;

    public ChatGPTService gptService = new ChatGPTService(OPEN_AI_TOKEN);

    public TinderBoltApp() {
        super(TELEGRAM_BOT_NAME, TELEGRAM_BOT_TOKEN);
    }

    @Override
    public void onUpdateEventReceived(Update update) {
        // основний функціонал бота 
        String message = getMessageText(); //повідомлення користувача з командою
        switch (message)
        {
            case "/start" ->{
                mode = DialogMode.MAIN;
                showMainMenu(
                "головне меню бота", "/start",
                "генерація Tinder-профілю 😎","/profile",
                "повідомлення для знайомства 🥰","/opener",
                "листування від вашого імені 😈","/message",
                "Листування з зірками 🔥","/date",
                "поставити запитання чату GPT 🧠","/gpt");
                sendPhotoMessage("main");
                String menu = loadMessage("main");
                sendTextMessage(menu);
                return;
            }
            case "/gpt" -> {
                mode = DialogMode.GPT;
                String gptMessage = loadMessage("gpt");
                sendTextMessage(gptMessage);
                sendPhotoMessage("gpt");
                return;
            }
            case "/date" -> {
                mode = DialogMode.DATE;
                String dateMessage = loadMessage("date");
                sendPhotoMessage("date");
                sendTextButtonsMessage(dateMessage,
                    "Аріана Гранде \uD83D\uDD25", "date_grande",
                    "Марго Роббі \uD83D\uDD25\uD83D\uDD25", "date_robbie",
                    "Зендея \uD83D\uDD25\uD83D\uDD25\uD83D\uDD25", "date_zendaya",
                    "Райан Гослінг \uD83D\uDE0E", "date_gosling",
                    "Том Харді \uD83D\uDE0E\uD83D\uDE0E", "date_hardy");
                return;
            }
            case "/message" -> {
                mode = DialogMode.MESSAGE;
                sendPhotoMessage("message");
                String gptMessageHelper = loadMessage("message");
                sendTextMessage(gptMessageHelper);
                sendTextButtonMessage( gptMessageHelper, 
                        "Наступне повідомлення", "message_next",
                        "Запросити на побачення", "message_date");

                chat = new ArrayList<>();
                return;
            }
            case "/profile" -> {
                mode = DialogMode.PROFILE;
                sendPhotoMessage("profile");
                String profileMessage = loadMessage("message");
                sendTextMessage(profileMessage);

                myInfo = new UserInfo();
                questionNumber = 1;
                sendTextMessage("Введіть ваше ім'я ");
                return;
            }
            case "/opener" -> {
                mode = DialogMode.OPENER;
                sendPhotoMessage("opener");
                String profileMessage = loadMessage("opener");
                sendTextMessage(profileMessage);

                personInfo = new UserInfo();
                questionNumber = 1;
                sendTextMessage("Введіть ім'я ");
                return;
            }
        }

        switch (mode) {
            case GPT ->{
                String prompt = loadPrompt("gpt");
                Message msg = sendTextMessage("Друкує...");
                String answer = gptService.sendMessage(prompt, message);
                updateTextMessage(msg, answer);
             
            }
            case DATE ->{
                String query = getCallbackQueryButtonKey();
                if (query.startsWith("date_")) 
                {
                    sendPhotoMessage(query);
                     String prompt = loadPrompt(query);
                    gptService.setPrompt(prompt);
                    return;
                }
                 Message msg = sendTextMessage("Почекай");
                 String answer = gptService.addMessage(message); // лист гпт у наявній гілці
                 updateTextMessage(msg, answer);
               
            }
            case MESSAGE ->{
                String query = getCallbackQueryButtonKey();
                if (query.startsWith("message_")) {
                    String prompt = loadPrompt(query);
                    String history = String.join("/n/n", chat);
                    Message msg = sendTextMessage("Друкує...");
                    String answer = gptService.sendMessage(prompt, history);
                    updateTextMessage(msg, answer);// спочатку msg, коли answer буде згенер, то він замінить msg
                }
            chat.add(message); //збереження історії повідомлень
        
            }
            case PROFILE -> {
                if (questionNumber <= 6)
                {
                    askQuestion(message, myInfo, "profile");
                }
            }
            case OPENER -> {
                
                 if (questionNumber <= 6)
                {
                    askQuestion(message, personInfo, "opener");
                }
            }
        }
    }
    
    private void askQuestion(String message, UserInfo user, String profileName) // формуємо анкету через GPT
    {
        switch(questionNumber)
        {
            case 1 -> {
                user.name = message;
                questionNumber = 2;
           sendTextMessage("Введіть вік: ");
            }
            case 2 -> {
                user.age = message;
                questionNumber = 3;
                sendTextMessage("Введіть місто: ");
           
            }case 3 -> {
                user.city = message;
                questionNumber = 4;
                sendTextMessage("Введіть професію: ");
            
            }
            case 4 -> {
                user.occupation = message;
                questionNumber = 5;
                sendTextMessage("Введіть хоббі: ");
            
            }
            case 5 -> {
                user.hobby = message;
                questionNumber = 6;
                sendTextMessage("Введіть цілі для знайомства: ");
          
            }
            case 6 -> {
                user.goals= message;
                String prompt = loadPrompt(profileName); 
                Message msg = sendTextMessage("ChatGPT друкує...");

                String answer = gptService.sendMessage(prompt, myInfo.toString());
                updateTextMessage(msg,answer);
            
            }

        }
    }

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(new TinderBoltApp());
    }

    private void sendTextButtonMessage(String gptMessageHelper, String наступне_повідомлення, String message_next, String запросити_на_побачення, String message_date) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}