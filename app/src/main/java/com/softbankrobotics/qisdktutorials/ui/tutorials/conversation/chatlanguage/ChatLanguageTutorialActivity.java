package com.softbankrobotics.qisdktutorials.ui.tutorials.conversation.chatlanguage;

import android.os.Bundle;
import android.util.Log;
import android.widget.RadioButton;

import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks;
import com.aldebaran.qi.sdk.builder.ChatBuilder;
import com.aldebaran.qi.sdk.builder.QiChatbotBuilder;
import com.aldebaran.qi.sdk.builder.SayBuilder;
import com.aldebaran.qi.sdk.builder.TopicBuilder;
import com.aldebaran.qi.sdk.object.conversation.Chat;
import com.aldebaran.qi.sdk.object.conversation.ConversationStatus;
import com.aldebaran.qi.sdk.object.conversation.QiChatbot;
import com.aldebaran.qi.sdk.object.conversation.Say;
import com.aldebaran.qi.sdk.object.conversation.Topic;
import com.aldebaran.qi.sdk.object.locale.Language;
import com.aldebaran.qi.sdk.object.locale.Locale;
import com.aldebaran.qi.sdk.object.locale.Region;
import com.softbankrobotics.qisdktutorials.R;
import com.softbankrobotics.qisdktutorials.ui.conversation.ConversationBinder;
import com.softbankrobotics.qisdktutorials.ui.conversation.ConversationItemType;
import com.softbankrobotics.qisdktutorials.ui.conversation.ConversationView;
import com.softbankrobotics.qisdktutorials.ui.tutorials.TutorialActivity;

/**
 * The tutorial for choosing a language for a Chat.
 */
public class ChatLanguageTutorialActivity extends TutorialActivity implements RobotLifecycleCallbacks {

    private static final String TAG = "ChatLanguageActivity";

    private RadioButton enButton;
    private RadioButton jaButton;
    private ConversationView conversationView;
    private ConversationBinder conversationBinder;

    // Store the Chat actions.
    private Chat chatEN;
    private Chat chatJA;
    // Store the action execution future.
    private Future<Void> discussion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        enButton = findViewById(R.id.enButton);
        jaButton = findViewById(R.id.jaButton);
        conversationView = findViewById(R.id.conversationView);

        // Change the language to English when checked.
        enButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                disableButtons();
                runChat(chatEN);
            }
        });

        // Change the language to Japanese when checked.
        jaButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                disableButtons();
                runChat(chatJA);
            }
        });

        // Register the RobotLifecycleCallbacks to this Activity.
        QiSDK.register(this, this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Disable and uncheck buttons.
        disableButtons();
        enButton.setChecked(false);
        jaButton.setChecked(false);
    }

    @Override
    protected void onDestroy() {
        // Unregister the RobotLifecycleCallbacks for this Activity.
        QiSDK.unregister(this, this);
        super.onDestroy();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_chat_language_tutorial;
    }

    @Override
    public void onRobotFocusGained(QiContext qiContext) {
        // Bind the conversational events to the view.
        ConversationStatus conversationStatus = qiContext.getConversation().status(qiContext.getRobotContext());
        conversationBinder = conversationView.bindConversationTo(conversationStatus);

        Say say = SayBuilder.with(qiContext)
                .withText("Select the language of the discussion and talk to me.")
                .build();

        say.run();

        // Prepare the Chat actions.
        buildEnglishChat(qiContext);
        buildJapaneseChat(qiContext);

        enableButtons();
    }

    @Override
    public void onRobotFocusLost() {
        if (conversationBinder != null) {
            conversationBinder.unbind();
        }

        // Remove the listeners from the Chat actions.
        if (chatEN != null) {
            chatEN.removeAllOnStartedListeners();
        }
        if (chatJA != null) {
            chatJA.removeAllOnStartedListeners();
        }
    }

    @Override
    public void onRobotFocusRefused(String reason) {
        // Nothing here.
    }

    private void buildEnglishChat(QiContext qiContext) {
        Locale locale = new Locale(Language.ENGLISH, Region.UNITED_STATES);
        chatEN = buildChat(qiContext, "hello-en.top", locale);
    }

    private void buildJapaneseChat(QiContext qiContext) {
        Locale locale = new Locale(Language.JAPANESE, Region.JAPAN);
        chatJA = buildChat(qiContext, "hello-ja.top", locale);
    }

    private Chat buildChat(QiContext qiContext, String topicAssetName, Locale locale) {
        // Create a topic from the asset file.
        Topic topic = TopicBuilder.with(qiContext)
                .withAsset(topicAssetName)
                .build();

        // Create a new QiChatbot with the specified Locale.
        QiChatbot qiChatbot = QiChatbotBuilder.with(qiContext)
                .withTopic(topic)
                .withLocale(locale)
                .build();

        // Create a new Chat action with the specified Locale.
        Chat chat = ChatBuilder.with(qiContext)
                .withChatbot(qiChatbot)
                .withLocale(locale)
                .build();

        // Enable buttons when the Chat starts.
        chat.addOnStartedListener(() -> {
            enableButtons();
            String message = "Discussion is now in " + locale.getLanguage() + ".";
            Log.i(TAG, message);
            displayLine(message, ConversationItemType.INFO_LOG);
        });

        return chat;
    }

    private void runChat(Chat chat) {
        // Cancel the current discussion if any, and then run the specified Chat.
        cancelFuture(discussion)
                .andThenConsume(ignored -> {
                    if (chat != null) {
                        discussion = chat.async().run();
                    }
                });
    }

    // Utility method used to cancel a Future and to be notified when the cancellation is done.
    private static Future<Void> cancelFuture(Future<?> futureToCancel) {
        if (futureToCancel == null) {
            return Future.of(null);
        }

        futureToCancel.requestCancellation();
        return futureToCancel.thenConsume(future -> {});
    }

    private void disableButtons() {
        runOnUiThread(() -> {
            enButton.setEnabled(false);
            jaButton.setEnabled(false);
        });
    }

    private void enableButtons() {
        runOnUiThread(() -> {
            enButton.setEnabled(true);
            jaButton.setEnabled(true);
        });
    }

    private void displayLine(final String text, final ConversationItemType type) {
        runOnUiThread(() -> conversationView.addLine(text, type));
    }
}
