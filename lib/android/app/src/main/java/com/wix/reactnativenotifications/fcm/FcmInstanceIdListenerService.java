package com.wix.reactnativenotifications.fcm;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.wix.reactnativenotifications.BuildConfig;
import com.wix.reactnativenotifications.core.notification.IPushNotification;
import com.wix.reactnativenotifications.core.notification.PushNotification;

import java.util.Map;
import io.intercom.android.sdk.push.IntercomPushClient;

import static com.wix.reactnativenotifications.Defs.LOGTAG;

/**
 * Instance-ID + token refreshing handling service. Contacts the FCM to fetch the updated token.
 *
 * @author amitd
 */
public class FcmInstanceIdListenerService extends FirebaseMessagingService {
    private final IntercomPushClient intercomPushClient = new IntercomPushClient();

    /**
     * Called when FCM refreshes the registration token. This can happen when:
     * - The app is restored on a new device
     * - The user uninstalls/reinstalls the app
     * - The user clears app data
     * - FCM determines that the token needs to be refreshed
     *
     * When this happens, we need to fetch the new token and notify the JS side
     * so the app can sync the new token with its backend server.
     *
     * @param token The new FCM registration token
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        if (BuildConfig.DEBUG) Log.d(LOGTAG, "FCM token refreshed by Firebase: " + token);

        // Trigger the token refresh handler to notify JS and any app-level listeners
        final Context appContext = getApplicationContext();
        final Intent tokenFetchIntent = new Intent(appContext, FcmInstanceIdRefreshHandlerService.class);
        // Don't set EXTRA_IS_APP_INIT or EXTRA_MANUAL_REFRESH - this uses the default path
        // which calls onNewTokenReady() to handle the automatic token refresh
        FcmInstanceIdRefreshHandlerService.enqueueWork(appContext, tokenFetchIntent);
    }

    @Override
    public void onMessageReceived(RemoteMessage message){
        Bundle bundle = message.toIntent().getExtras();
        if(BuildConfig.DEBUG) Log.d(LOGTAG, "New message from FCM: " + bundle);

        Map<String, String> mess = message.getData();
        if (intercomPushClient.isIntercomPush(mess)) {
          intercomPushClient.handlePush(getApplication(), mess);
          return;
        }

        try {
            final IPushNotification notification = PushNotification.get(getApplicationContext(), bundle);
            notification.onReceived();
        } catch (IPushNotification.InvalidNotificationException e) {
            // An FCM message, yes - but not the kind we know how to work with.
            if(BuildConfig.DEBUG) Log.v(LOGTAG, "FCM message handling aborted", e);
        }
    }
}
