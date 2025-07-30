package com.buc.mealmate;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.telephony.SmsManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class SmsHelper {

    private static final int SMS_PERMISSION_REQUEST = 101;

    public static void sendSms(Activity activity, String phoneNumber, String message) {
        // Check for permission first
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            // Request permission, then retry after user responds
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.SEND_SMS},
                    SMS_PERMISSION_REQUEST);
            Toast.makeText(activity, "Permission required to send SMS", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Try to send SMS directly
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Toast.makeText(activity, "SMS Sent", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            // If direct sending fails (Android 10+ or no SIM), open default SMS app by itself
            openSmsApp(activity, phoneNumber, message);
        }
    }

    /**
     * Opens the SMS app with prefilled recipient and message (always works).
     */
    private static void openSmsApp(Activity activity, String phoneNumber, String message) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("sms:" + phoneNumber));
            intent.putExtra("sms_body", message);
            activity.startActivity(intent);
            Toast.makeText(activity, "Opening SMS app...", Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            Toast.makeText(activity, "Failed to open SMS app", Toast.LENGTH_SHORT).show();
        }
    }
}
