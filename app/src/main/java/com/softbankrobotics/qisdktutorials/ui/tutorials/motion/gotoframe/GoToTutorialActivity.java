/*
 * Copyright (C) 2018 Softbank Robotics Europe
 * See COPYING for the license
 */

package com.softbankrobotics.qisdktutorials.ui.tutorials.motion.gotoframe;

import android.os.Bundle;
import android.util.Log;

import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks;
import com.aldebaran.qi.sdk.builder.GoToBuilder;
import com.aldebaran.qi.sdk.builder.SayBuilder;
import com.aldebaran.qi.sdk.builder.TransformBuilder;
import com.aldebaran.qi.sdk.object.actuation.Actuation;
import com.aldebaran.qi.sdk.object.actuation.Frame;
import com.aldebaran.qi.sdk.object.actuation.FreeFrame;
import com.aldebaran.qi.sdk.object.actuation.GoTo;
import com.aldebaran.qi.sdk.object.actuation.Mapping;
import com.aldebaran.qi.sdk.object.conversation.ConversationStatus;
import com.aldebaran.qi.sdk.object.conversation.Say;
import com.aldebaran.qi.sdk.object.geometry.Transform;
import com.softbankrobotics.qisdktutorials.R;
import com.softbankrobotics.qisdktutorials.ui.conversation.ConversationBinder;
import com.softbankrobotics.qisdktutorials.ui.conversation.ConversationItemType;
import com.softbankrobotics.qisdktutorials.ui.conversation.ConversationView;
import com.softbankrobotics.qisdktutorials.ui.tutorials.TutorialActivity;

/**
 * The activity for the GoTo tutorial.
 */
public class GoToTutorialActivity extends TutorialActivity implements RobotLifecycleCallbacks {

    private static final String TAG = "GoToTutorialActivity";

    private ConversationView conversationView;
    private ConversationBinder conversationBinder;

    // Store the GoTo action.
    private GoTo goTo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        conversationView = findViewById(R.id.conversationView);

        // Register the RobotLifecycleCallbacks to this Activity.
        QiSDK.register(this, this);
    }

    @Override
    protected void onDestroy() {
        // Unregister the RobotLifecycleCallbacks for this Activity.
        QiSDK.unregister(this, this);
        super.onDestroy();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.conversation_layout;
    }

    @Override
    public void onRobotFocusGained(QiContext qiContext) {
        // Bind the conversational events to the view.
        ConversationStatus conversationStatus = qiContext.getConversation().status(qiContext.getRobotContext());
        conversationBinder = conversationView.bindConversationTo(conversationStatus);

        Say say = SayBuilder.with(qiContext)
                .withText("I can move around: I will go 1 meter forward.")
                .build();

        say.run();

        // Get the Actuation service from the QiContext.
        Actuation actuation = qiContext.getActuation();

        // Get the robot frame.
        Frame robotFrame = actuation.robotFrame();

        // Create a transform corresponding to a 1 meter forward translation.
        Transform transform = TransformBuilder.create()
                                              .fromXTranslation(1);

        // Get the Mapping service from the QiContext.
        Mapping mapping = qiContext.getMapping();

        // Create a FreeFrame with the Mapping service.
        FreeFrame targetFrame = mapping.makeFreeFrame();

        // Update the target location relatively to Pepper's current location.
        targetFrame.update(robotFrame, transform, System.currentTimeMillis());

        // Create a GoTo action.
        goTo = GoToBuilder.with(qiContext) // Create the builder with the QiContext.
                               .withFrame(targetFrame.frame()) // Set the target frame.
                               .build(); // Build the GoTo action.

        // Add an on started listener on the GoTo action.
        goTo.addOnStartedListener(() -> {
            String message = "GoTo action started.";
            Log.i(TAG, message);
            displayLine(message, ConversationItemType.INFO_LOG);
        });

        // Execute the GoTo action asynchronously.
        Future<Void> goToFuture = goTo.async().run();

        // Add a lambda to the action execution.
        goToFuture.thenConsume(future -> {
            if (future.isSuccess()) {
                String message = "GoTo action finished with success.";
                Log.i(TAG, message);
                displayLine(message, ConversationItemType.INFO_LOG);
            } else if (future.hasError()) {
                String message = "GoTo action finished with error.";
                Log.e(TAG, message, future.getError());
                displayLine(message, ConversationItemType.ERROR_LOG);
            }
        });
    }

    @Override
    public void onRobotFocusLost() {
        if (conversationBinder != null) {
            conversationBinder.unbind();
        }

        // Remove on started listeners from the GoTo action.
        if (goTo != null) {
            goTo.removeAllOnStartedListeners();
        }
    }

    @Override
    public void onRobotFocusRefused(String reason) {
        // Nothing here.
    }

    private void displayLine(final String text, final ConversationItemType type) {
        runOnUiThread(() -> conversationView.addLine(text, type));
    }
}
