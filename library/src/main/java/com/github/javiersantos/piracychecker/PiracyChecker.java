package com.github.javiersantos.piracychecker;

import android.content.Context;
import android.provider.Settings;
import android.support.annotation.StringRes;

import com.github.javiersantos.piracychecker.enums.InstallerID;
import com.github.javiersantos.piracychecker.enums.PiracyCheckerCallback;
import com.github.javiersantos.piracychecker.enums.PiracyCheckerError;
import com.google.android.vending.licensing.AESObfuscator;
import com.google.android.vending.licensing.LicenseChecker;
import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.google.android.vending.licensing.ServerManagedPolicy;

import java.util.ArrayList;
import java.util.List;

public class PiracyChecker {
    private Context context;
    private String unlicensedDialogTitle, unlicensedDialogDescription;
    private boolean enableLVL, enableSigningCertificate, enableInstallerId;
    private String licenseBase64;
    private String signature;
    private List<InstallerID> installerIDs;
    private PiracyCheckerCallback callback;

    public PiracyChecker(Context context) {
        this.context = context;
        this.unlicensedDialogTitle = context.getString(R.string.app_unlicensed);
        this.unlicensedDialogDescription = context.getString(R.string.app_unlicensed_description);
        this.installerIDs = new ArrayList<>();
    }

    public PiracyChecker(Context context, String title, String description) {
        this.context = context;
        this.unlicensedDialogTitle = title;
        this.unlicensedDialogDescription = description;
        this.installerIDs = new ArrayList<>();
    }

    public PiracyChecker(Context context, @StringRes int title, @StringRes int description) {
        new PiracyChecker(context, context.getString(title), context.getString(description));
    }

    public PiracyChecker enableGooglePlayLicensing(String licenseKeyBase64) {
        this.enableLVL = true;
        this.licenseBase64 = licenseKeyBase64;
        return this;
    }

    public PiracyChecker enableSigningCertificate(String signature) {
        this.enableSigningCertificate = true;
        this.signature = signature;
        return this;
    }

    public PiracyChecker enableInstallerId(InstallerID installerID) {
        this.enableInstallerId = true;
        this.installerIDs.add(installerID);
        return this;
    }

    public PiracyChecker callback(PiracyCheckerCallback callback) {
        this.callback = callback;
        return this;
    }

    public void start() {
        if (callback != null)
            verify(callback);
        else
            verify(new PiracyCheckerCallback() {
                @Override
                public void allow() {}

                @Override
                public void dontAllow(PiracyCheckerError error) {
                    UtilsLibrary.showUnlicensedDialog(context, unlicensedDialogTitle, unlicensedDialogDescription).show();
                }

				@Override
                public void applicationError(int errorCode){}
            });
    }

    private void verify(final PiracyCheckerCallback verifyCallback) {
        // Library will verify first the non-LVL methods since LVL is asynchronous and could take some seconds to give a result
        if (!verifySigningCertificate()) {
            verifyCallback.dontAllow(PiracyCheckerError.SIGNATURE_NOT_VALID);
        } else if (!verifyInstallerId()) {
            verifyCallback.dontAllow(PiracyCheckerError.INVALID_INSTALLER_ID);
        } else {
            if (enableLVL) {
                String deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
                LicenseChecker licenseChecker = new LicenseChecker(context, new ServerManagedPolicy(context, new AESObfuscator(UtilsLibrary.SALT, context.getPackageName(), deviceId)), licenseBase64);
                licenseChecker.checkAccess(new LicenseCheckerCallback() {
                    @Override
                    public void allow(int reason) {
                        verifyCallback.allow();
                    }

                    @Override
                    public void dontAllow(int reason) {
                        verifyCallback.dontAllow(PiracyCheckerError.NOT_LICENSED);
                    }

                    @Override
                    public void applicationError(int errorCode) {
                    	verifyCallback.applicationError(errorCode);
                    }
                });
            } else {
                verifyCallback.allow();
            }
        }
    }

    private boolean verifySigningCertificate() {
        boolean signingVerifyValid = false;

        if (enableSigningCertificate) {
            if (UtilsLibrary.verifySigningCertificate(context, signature)) {
                signingVerifyValid = true;
            }
        } else {
            signingVerifyValid = true;
        }

        return signingVerifyValid;
    }

    private boolean verifyInstallerId() {
        boolean installerIdValid = false;

        if (enableInstallerId) {
            if (UtilsLibrary.verifyInstallerId(context, installerIDs)) {
                installerIdValid = true;
            }
        } else {
            installerIdValid = true;
        }

        return installerIdValid;
    }

}
