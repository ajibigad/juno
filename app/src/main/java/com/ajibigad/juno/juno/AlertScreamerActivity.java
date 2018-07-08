package com.ajibigad.juno.juno;

import android.hardware.fingerprint.FingerprintManager;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.ajibigad.juno.juno.utils.FingerPrintUtils;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class AlertScreamerActivity extends AppCompatActivity implements FingerprintUiHelper.Callback {

    private KeyStore mKeyStore;
    private KeyGenerator mKeyGenerator;
    private static String TAG = AlertScreamerActivity.class.getSimpleName();

    private static final String SECRET_MESSAGE = "Gidi juno secret ise";
    static final String DEFAULT_KEY_NAME = "nomo_juno__key";

    private static final String DIALOG_FRAGMENT_TAG = "fingerprintFragment";

    private Ringtone ringtone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //scream, accept fingerprint to either snooze or stop screaming
        // if user snoozes, then set its next triggerTime say 5 mins
        // if he stops, get alert using its id, check if its a repeat, if not delete
        // alert id and message are in the bundle's extra
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_screamer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //displays the alert message
        TextView textView = (TextView) findViewById(R.id.alert_message);
        String alertMessage = (String) getIntent().getExtras().get("alertMessage");
        textView.setText(alertMessage);

        if (FingerPrintUtils.confirmFingerprintAuthenticationSetup(this)) {
            Cipher cipher = setupFingerPrintAuthenticationCipher();

            Button stopScreamBtn = (Button) findViewById(R.id.stopScreamBtn);
            stopScreamBtn.setOnClickListener(
                    new StopScreamButtonClickListener(cipher, DEFAULT_KEY_NAME));
        }
        scream();
    }

    private void scream() {
//        mediaPlayer = MediaPlayer.create(AlertScreamerActivity.this, R.raw.alert);
//        mediaPlayer.setLooping(true);
//        mediaPlayer.start();

        Uri ringtoneUri = Uri.parse("android.resource://com.ajibigad.juno.juno/" + R.raw.alert);
        ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
        AudioAttributes audioAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build();
        ringtone.setAudioAttributes(audioAttributes);
        ringtone.play();
        Log.i(TAG, "Playing ringtone");
    }

    private Cipher setupFingerPrintAuthenticationCipher() {
        try {
            mKeyStore = KeyStore.getInstance("AndroidKeyStore");
        } catch (KeyStoreException e) {
            throw new RuntimeException("Failed to get an instance of KeyStore", e);
        }

        try {
            mKeyGenerator = KeyGenerator
                    .getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException("Failed to get an instance of KeyGenerator", e);
        }

        Cipher cipher;
        try {
            cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException("Failed to get an instance of Cipher", e);
        }

        createKey(DEFAULT_KEY_NAME, true);

        return cipher;
    }

    /**
     * Creates a symmetric key in the Android Key Store which can only be used after the user has
     * authenticated with fingerprint.
     *
     * @param keyName                          the name of the key to be created
     * @param invalidatedByBiometricEnrollment if {@code false} is passed, the created key will not
     *                                         be invalidated even if a new fingerprint is enrolled.
     *                                         The default value is {@code true}, so passing
     *                                         {@code true} doesn't change the behavior
     *                                         (the key will be invalidated if a new fingerprint is
     *                                         enrolled.). Note that this parameter is only valid if
     *                                         the app works on Android N developer preview.
     */
    public void createKey(String keyName, boolean invalidatedByBiometricEnrollment) {
        // The enrolling flow for fingerprint. This is where you ask the user to set up fingerprint
        // for your flow. Use of keys is necessary if you need to know if the set of
        // enrolled fingerprints has changed.
        try {
            mKeyStore.load(null);
            // Set the alias of the entry in Android KeyStore where the key will appear
            // and the constrains (purposes) in the constructor of the Builder

            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(keyName,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    // Require the user to authenticate with a fingerprint to authorize every use
                    // of the key
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7);

            // This is a workaround to avoid crashes on devices whose API level is < 24
            // because KeyGenParameterSpec.Builder#setInvalidatedByBiometricEnrollment is only
            // visible on API level +24.
            // Ideally there should be a compat library for KeyGenParameterSpec.Builder but
            // which isn't available yet.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setInvalidatedByBiometricEnrollment(invalidatedByBiometricEnrollment);
            }
            mKeyGenerator.init(builder.build());
            mKeyGenerator.generateKey();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException
                | CertificateException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Tries to encrypt some data with the generated key in {@link #createKey} which
     * only works if the user has just authenticated via fingerprint.
     */
    private boolean confirmFingerprintCipher(Cipher cipher) {
        try {
            byte[] encrypted = cipher.doFinal(SECRET_MESSAGE.getBytes());
            if (encrypted != null) {
                Log.i(TAG, Base64.encodeToString(encrypted, 0));
            }
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            Toast.makeText(this, "Failed to encrypt the data with the generated key. "
                    + "Retry the operation", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Failed to encrypt the data with the generated key." + e.getMessage());
            return false;
        }
        return true;
    }

    private void postAuthentication() {
        stopScreaming();
    }

    @Override
    public void onAuthenticated(@Nullable FingerprintManager.CryptoObject cryptoObject) {
        // If the user has authenticated with fingerprint, verify that using cryptography and
        // then show the confirmation message.
        if (cryptoObject == null) {
            Toast.makeText(this, "Fingerprint Authentication Failed", Toast.LENGTH_SHORT).show();
            return;
        }

        if (confirmFingerprintCipher(cryptoObject.getCipher())) {
            postAuthentication();
        }
    }

    @Override
    public void onError(String errorMessage) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

    private class StopScreamButtonClickListener implements View.OnClickListener {

        Cipher mCipher;
        String mKeyName;

        StopScreamButtonClickListener(Cipher cipher, String keyName) {
            mCipher = cipher;
            mKeyName = keyName;
        }

        @Override
        public void onClick(View view) {
            // Set up the crypto object for later. The object will be authenticated by use
            // of the fingerprint.

            if (mCipher == null) {
                stopScreaming();
                return;
            }

            if (initCipher(mCipher, mKeyName)) {
                // Show the fingerprint dialog. The user has the option to use the fingerprint with crypto
                FingerprintAuthenticationDialogFragment fragment
                        = new FingerprintAuthenticationDialogFragment();
                fragment.setCryptoObject(new FingerprintManager.CryptoObject(mCipher));
                fragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
            } else {
                // This happens if the lock screen has been disabled or a new fingerprint got
                // enrolled.
                Toast.makeText(AlertScreamerActivity.this,
                        "Either Lock screen has been disabled or new fingerprint has been enrolled",
                        Toast.LENGTH_LONG).show();
            }
        }

        /**
         * Initialize the {@link Cipher} instance with the created key in the
         * {@link #createKey(String, boolean)} method.
         *
         * @param keyName the key name to init the cipher
         * @return {@code true} if initialization is successful, {@code false} if the lock screen has
         * been disabled or reset after the key was generated, or if a fingerprint got enrolled after
         * the key was generated.
         */
        private boolean initCipher(Cipher cipher, String keyName) {
            try {
                mKeyStore.load(null);
                SecretKey key = (SecretKey) mKeyStore.getKey(keyName, null);
                cipher.init(Cipher.ENCRYPT_MODE, key);
                return true;
            } catch (KeyPermanentlyInvalidatedException e) {
                return false;
            } catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException
                    | NoSuchAlgorithmException | InvalidKeyException e) {
                throw new RuntimeException("Failed to init Cipher", e);
            }
        }
    }

    private void stopScreaming() {
        ringtone.stop();
        finish();
    }

//    @Override
//    public void onStop() {
//        super.onStop();
//        if (mediaPlayer != null) mediaPlayer.release();
//    }

}
