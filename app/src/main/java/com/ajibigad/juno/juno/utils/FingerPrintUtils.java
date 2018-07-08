package com.ajibigad.juno.juno.utils;

import android.app.KeyguardManager;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.widget.Toast;

import static android.content.Context.FINGERPRINT_SERVICE;

public class FingerPrintUtils {

    public static boolean isFingerprintAuthenticationAvailable(Context context) {
        FingerprintManager fingerprintManager = (FingerprintManager) context.getSystemService(FINGERPRINT_SERVICE);
        return fingerprintManager.isHardwareDetected()
                && fingerprintManager.hasEnrolledFingerprints();
    }

    public static boolean confirmFingerprintAuthenticationSetup(Context context) {
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        FingerprintManager fingerprintManager = (FingerprintManager) context.getSystemService(FINGERPRINT_SERVICE);
        if (!keyguardManager.isKeyguardSecure()) {
            // Show a message that the user hasn't set up a fingerprint or lock screen.
            Toast.makeText(context.getApplicationContext(),
                    "Finger print cannot be used. Secure lock screen has not been set up.\n"
                            + "Go to 'Settings -> Security -> Fingerprint' to set up a fingerprint",
                    Toast.LENGTH_LONG).show();
            return false;
        }

        // Now the protection level of USE_FINGERPRINT permission is normal instead of dangerous.
        // See http://developer.android.com/reference/android/Manifest.permission.html#USE_FINGERPRINT
        // The line below prevents the false positive inspection from Android Studio
        // noinspection ResourceType
        if (!fingerprintManager.hasEnrolledFingerprints()) {
            // This happens when no fingerprints are registered.
            Toast.makeText(context.getApplicationContext(),
                    "Go to 'Settings -> Security -> Fingerprint' and register at least one fingerprint",
                    Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }
}
