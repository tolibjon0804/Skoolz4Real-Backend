package com.skoolz4real.backend.telegram;

import com.skoolz4real.backend.controller.FetchScheduleController;
import com.skoolz4real.backend.model.StudentCredentials;
import com.skoolz4real.backend.model.Course;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class Skoolz4RealBot extends TelegramLongPollingBot {

    @Value("${bot.username}")
    private String botUsername;

    @Value("${bot.token}")
    private String botToken;
    
    @Autowired
    private FetchScheduleController fetchScheduleController;
    
    // Store user states - which step they're at in the conversation
    private final Map<Long, String> userStates = new HashMap<>();
    
    // Store credentials temporarily
    private final Map<Long, String> usernames = new HashMap<>();
    private final Map<Long, String> passwords = new HashMap<>();
    
    // Store schedules by chat ID
    private final Map<Long, List<Course>> userSchedule = new HashMap<>();

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        // Handle text messages and commands
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageText.equals("/start")) {
                sendStartMessage(chatId);
            } else {
                processUserInput(chatId, messageText);
            }
        } 
        // Handle inline keyboard callbacks
        else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            
            processCallbackQuery(chatId, callbackData);
        }
        // Handle photo uploads
        else if (update.hasMessage() && update.getMessage().hasPhoto()) {
            processPhotoUpload(update);
        }
    }
    
    private void sendStartMessage(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Welcome to Skoolz4Real Bot! 📚\n\nHow would you like to add your schedule?");
        
        // Create inline keyboard buttons
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        
        // First row of buttons
        List<InlineKeyboardButton> rowInline1 = new ArrayList<>();
        
        // Upload Image button
        InlineKeyboardButton uploadImageButton = new InlineKeyboardButton();
        uploadImageButton.setText("Upload Schedule Image 📷");
        uploadImageButton.setCallbackData("UPLOAD_IMAGE");
        rowInline1.add(uploadImageButton);
        
        // Add the first row to the keyboard
        rowsInline.add(rowInline1);
        
        // Second row of buttons
        List<InlineKeyboardButton> rowInline2 = new ArrayList<>();
        
        // Login Credentials button
        InlineKeyboardButton credentialsButton = new InlineKeyboardButton();
        credentialsButton.setText("Enter University Credentials 🔑");
        credentialsButton.setCallbackData("ENTER_CREDENTIALS");
        rowInline2.add(credentialsButton);
        
        // Add the second row to the keyboard
        rowsInline.add(rowInline2);
        
        // Set the keyboard to the markup
        markupInline.setKeyboard(rowsInline);
        
        // Add the markup to the message
        message.setReplyMarkup(markupInline);
        
        // Reset user state
        userStates.put(chatId, "AWAITING_CHOICE");
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void processCallbackQuery(long chatId, String callbackData) {
        switch (callbackData) {
            case "UPLOAD_IMAGE":
                sendMessage(chatId, "Please upload an image of your schedule.");
                userStates.put(chatId, "AWAITING_IMAGE");
                break;
                
            case "ENTER_CREDENTIALS":
                sendMessage(chatId, "Please enter your university username:");
                userStates.put(chatId, "AWAITING_USERNAME");
                break;
                
            default:
                sendMessage(chatId, "Unknown option. Please try again with /start");
                break;
        }
    }
    
    private void processUserInput(long chatId, String messageText) {
        String userState = userStates.getOrDefault(chatId, "");
        
        switch (userState) {
            case "AWAITING_USERNAME":
                // Save username and ask for password
                usernames.put(chatId, messageText);
                userStates.put(chatId, "AWAITING_PASSWORD");
                sendMessage(chatId, "Please enter your university password:");
                break;
                
            case "AWAITING_PASSWORD":
                // Save password and process login
                passwords.put(chatId, messageText);
                userStates.put(chatId, "PROCESSING");
                processLogin(chatId);
                break;
                
            default:
                if (!messageText.startsWith("/")) {
                    sendMessage(chatId, "Please use /start to begin the process.");
                }
                break;
        }
    }
    
    private void processPhotoUpload(Update update) {
        long chatId = update.getMessage().getChatId();
        if (userStates.getOrDefault(chatId, "").equals("AWAITING_IMAGE")) {
            sendMessage(chatId, "Thanks for uploading your schedule image! We'll process it shortly.");
            // Here you would add code to process the image
            // Future: Implement OCR to extract schedule data from the image
            userStates.put(chatId, "PROCESSED");
        } else {
            sendMessage(chatId, "Please use /start to begin the process before uploading an image.");
        }
    }
    
    private void processLogin(long chatId) {
        String username = usernames.get(chatId);
        String password = passwords.get(chatId);
        
        sendMessage(chatId, "Processing your credentials. Please wait...");
        
        try {
            // Create login request
            StudentCredentials studentCredentials = new StudentCredentials();
            studentCredentials.setUsername(username);
            studentCredentials.setPassword(password);
            
            // Use the LoginController to fetch and parse the schedule
            List<Course> schedule = fetchScheduleController.loginAndFetchSchedule(studentCredentials);
            
            // Clear sensitive data
            passwords.remove(chatId);
            
            if (schedule != null && !schedule.isEmpty()) {
                // Store the schedules for this user
                userSchedule.put(chatId, schedule);
                
                // Send a success message with a schedule summary
                StringBuilder sb = new StringBuilder();
                sb.append("✅ Successfully fetched your schedule!\n\n");
                sb.append("📚 You have ").append(schedule.size()).append(" courses in your schedule:\n\n");
                
                for (Course course : schedule) {
                    sb.append("📘 ").append(course.getTitle()).append("\n");
                    sb.append("🕒 ").append(course.getDays()).append(" - ").append(course.getTime()).append("\n");
                    sb.append("🏫 ").append(course.getCampus()).append(" - ").append(course.getRoom()).append("\n\n");
                }
                
                sendMessage(chatId, sb.toString());
                userStates.put(chatId, "PROCESSED");
            } else {
                sendMessage(chatId, "No schedule found or unable to parse schedule data. Please check your credentials and try again with /start");
                userStates.remove(chatId);
            }
        } catch (Exception e) {
            sendMessage(chatId, "An error occurred while fetching your schedule: " + e.getMessage());
            userStates.remove(chatId);
        }
    }
    
    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}