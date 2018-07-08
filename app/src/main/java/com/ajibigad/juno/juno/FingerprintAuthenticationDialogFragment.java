/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.ajibigad.juno.juno;

import android.app.DialogFragment;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ajibigad.juno.juno.utils.FingerPrintUtils;

/**
 * A dialog which uses fingerprint APIs to authenticate the user
 */
public class FingerprintAuthenticationDialogFragment extends DialogFragment
        implements FingerprintUiHelper.Callback {

    private Button mCancelButton;
    private View mFingerprintContent;

    private FingerprintManager.CryptoObject mCryptoObject;
    private FingerprintUiHelper mFingerprintUiHelper;
    private AlertScreamerActivity mActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Do not create a new Fragment when the Activity is re-created such as orientation changes.
        setRetainInstance(true);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().setTitle(getString(R.string.stop_scream));
        View v = inflater.inflate(R.layout.fingerprint_dialog_container, container, false);
        mCancelButton = (Button) v.findViewById(R.id.cancel_button);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        mFingerprintContent = v.findViewById(R.id.fingerprint_container);

        mFingerprintUiHelper = new FingerprintUiHelper(
                mActivity.getSystemService(FingerprintManager.class),
                (ImageView) v.findViewById(R.id.fingerprint_icon),
                (TextView) v.findViewById(R.id.fingerprint_status), this);
        mCancelButton.setText(R.string.cancel);
        mFingerprintContent.setVisibility(View.VISIBLE);

        if (!FingerPrintUtils.isFingerprintAuthenticationAvailable(getContext())) {
            //use lock screen
            onError("Fingerprint authentication is not available on your device");
            dismiss();
        }
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        mFingerprintUiHelper.startListening(getContext(), mCryptoObject);
    }

    @Override
    public void onPause() {
        super.onPause();
        mFingerprintUiHelper.stopListening();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (AlertScreamerActivity) getActivity();
    }

    /**
     * Sets the crypto object to be passed in when authenticating with fingerprint.
     */
    public void setCryptoObject(FingerprintManager.CryptoObject cryptoObject) {
        mCryptoObject = cryptoObject;
    }

    @Override
    public void onAuthenticated(@Nullable FingerprintManager.CryptoObject cryptoObject) {
        mActivity.onAuthenticated(mCryptoObject);
        dismiss();
    }

    @Override
    public void onError(String errorMessage) {
        Toast.makeText(mActivity, errorMessage, Toast.LENGTH_LONG).show();
        dismiss();
    }

    /**
     * Enumeration to indicate which authentication method the user is trying to authenticate with.
     */
//    public enum Stage {
//        FINGERPRINT,
//        NEW_FINGERPRINT_ENROLLED,
//        LOCK_SCREEN
//    }
}
