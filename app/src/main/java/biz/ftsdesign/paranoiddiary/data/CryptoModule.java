package biz.ftsdesign.paranoiddiary.data;

import androidx.annotation.NonNull;

import java.security.GeneralSecurityException;

interface CryptoModule {
    @NonNull
    byte[] encrypt(@NonNull String plainText) throws GeneralSecurityException;

    @NonNull
    byte[] encrypt(@NonNull byte[] plainTextBytes) throws GeneralSecurityException;

    @NonNull
    byte[] decryptBytes(@NonNull byte[] cipherText) throws GeneralSecurityException;

    @NonNull
    String decrypt(@NonNull byte[] cipherText) throws GeneralSecurityException;

    int getMinLength();
}
