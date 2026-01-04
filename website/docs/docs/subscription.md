---
id: subscription
title: Push Notifications Subscription
sidebar_label: Subscription
---

The typical flow for subscribing a device for receiving push notification in real time is to first register the device at the vendor's servers (e.g. FCM), then publishing the received token to your own push management servers.

This section is about the first part of the flow.

In order to handle notifications, you must register the `remoteNotificationsRegistered` event beforehand.

## Important: Token Refresh Handling

:::caution Critical for Android
FCM (Firebase Cloud Messaging) can refresh the device token at any time, not just during initial registration. This happens when:
- The app is restored on a new device
- The user reinstalls the app
- The user clears app data
- FCM determines the token needs to be refreshed (e.g., after extended inactivity)
- Security-related token rotation

**Your app must always sync the token to your backend whenever `remoteNotificationsRegistered` fires**, not just the first time. Failure to do so will cause push notifications to silently stop working for affected users.
:::

In your React Native app:

```jsx
import { Notifications } from 'react-native-notifications';

class App extends Component {
	constructor() {
		// Request permissions on iOS, refresh token on Android
		Notifications.registerRemoteNotifications();

		Notifications.events().registerRemoteNotificationsRegistered((event: Registered) => {
			// IMPORTANT: Always sync the token to your backend!
			// This event fires on initial registration AND on token refresh.
			console.log("Device Token Received/Refreshed", event.deviceToken);
			this.syncTokenToBackend(event.deviceToken);
		});
		Notifications.events().registerRemoteNotificationsRegistrationFailed((event: RegistrationError) => {
			console.error(event);
		});
	}
	
	async syncTokenToBackend(token: string) {
		// POST the token to your server every time - even if you think it hasn't changed
		await fetch('https://your-server.com/api/push-token', {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify({ token }),
		});
	}
}

```

### React Hooks Example

```jsx
import { useEffect } from 'react';
import { Notifications } from 'react-native-notifications';

function App() {
	useEffect(() => {
		// Set up the listener BEFORE calling registerRemoteNotifications
		const subscription = Notifications.events().registerRemoteNotificationsRegistered((event) => {
			console.log("Device Token Received", event.deviceToken);
			
			// On Android, you can check if this is an FCM-initiated refresh
			if (event.isRefresh) {
				console.log("Token was refreshed by FCM - old token is now invalid");
			}
			
			// Always sync the token to your backend
			syncTokenToBackend(event.deviceToken, { isRefresh: event.isRefresh });
		});
		
		const errorSubscription = Notifications.events().registerRemoteNotificationsRegistrationFailed((event) => {
			console.error("Registration failed", event);
		});

		Notifications.registerRemoteNotifications();

		return () => {
			subscription.remove();
			errorSubscription.remove();
		};
	}, []);
	
	// ...
}
```

### The `isRefresh` Property (Android Only)

On Android, the `Registered` event includes an `isRefresh` boolean property:

- `true`: The token was refreshed by FCM automatically. This means the previous token is now **invalid** and you must update your backend immediately.
- `false`: The token was received during app initialization or a manual refresh request.
- `undefined`: On iOS, this property is not set.

```typescript
interface Registered {
  deviceToken: string;
  isRefresh?: boolean; // Android only
}
```

When you have the device token, POST it to your server and register the device in your notifications provider (Amazon SNS, Azure, etc.).

You can check if the user granted permissions on iOS by calling `checkPermissions()`:

```jsx
Notifications.ios.checkPermissions().then((currentPermissions) => {
    console.log('Badges enabled: ' + !!currentPermissions.badge);
    console.log('Sounds enabled: ' + !!currentPermissions.sound);
    console.log('Alerts enabled: ' + !!currentPermissions.alert);
    console.log('Car Play enabled: ' + !!currentPermissions.carPlay);
    console.log('Critical Alerts enabled: ' + !!currentPermissions.criticalAlert);
    console.log('Provisional enabled: ' + !!currentPermissions.provisional);
    console.log('Provides App Notification Settings enabled: ' + !!currentPermissions.providesAppNotificationSettings);
	console.log('Announcement enabled: ' + !!currentPermissions.announcement);
});
```
