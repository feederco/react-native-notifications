import { Notification } from '../DTO/Notification';
import { NotificationActionResponse } from './NotificationActionResponse';

export interface Registered {
  deviceToken: string;
  /**
   * Indicates whether this token was received due to an FCM-initiated refresh.
   * 
   * - `true`: FCM automatically refreshed the token (e.g., due to security rotation,
   *   extended inactivity, or other FCM-internal reasons). The old token is now invalid.
   * - `false`: Token received during app initialization or manual refresh request.
   * 
   * **Important**: Always sync the token to your backend regardless of this value,
   * but you may want to log or handle refresh events differently for debugging purposes.
   * 
   * Note: This property is only set on Android. On iOS, it will always be `undefined`.
   */
  isRefresh?: boolean;
}

export interface RegistrationError {
  code: string;
  domain: string;
  localizedDescription: string;
}

export interface RegisteredPushKit {
  pushKitToken: string;
}

export interface NotificationResponse {
  identifier: string;
  notification: Notification;
  action?: NotificationActionResponse
}
