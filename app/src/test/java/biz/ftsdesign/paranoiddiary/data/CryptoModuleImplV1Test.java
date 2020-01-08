package biz.ftsdesign.paranoiddiary.data;

import org.junit.Test;

import java.security.GeneralSecurityException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class CryptoModuleImplV1Test {
    @Test
    public void testTwoWays() throws GeneralSecurityException {
        String plaintext = "1234";
        CryptoModuleImplV1 crypto = new CryptoModuleImplV1("password".toCharArray());
        byte[] ciphertext = crypto.encrypt(plaintext);
        String decrypted = crypto.decrypt(ciphertext);
        assertEquals(plaintext, decrypted);
    }

    @Test
    public void testTwoWaysDifferentInstance() throws GeneralSecurityException {
        String plaintext = "1234";
        CryptoModuleImplV1 crypto1 = new CryptoModuleImplV1("password".toCharArray());
        byte[] ciphertext = crypto1.encrypt(plaintext);

        CryptoModuleImplV1 crypto2 = new CryptoModuleImplV1("password".toCharArray());
        String decrypted = crypto2.decrypt(ciphertext);
        assertEquals(plaintext, decrypted);
    }

    @Test
    public void testSameMessage() throws GeneralSecurityException {
        final String plaintext = "1234";
        CryptoModuleImplV1 crypto = new CryptoModuleImplV1("password".toCharArray());
        byte[] ciphertext1 = crypto.encrypt(plaintext);
        byte[] ciphertext2 = crypto.encrypt(plaintext);
        assertFalse(Arrays.equals(ciphertext1, ciphertext2));
        assertFalse(Arrays.equals(CryptoModuleImplV1.extractIV(ciphertext1), CryptoModuleImplV1.extractIV(ciphertext2)));
        assertFalse(Arrays.equals(CryptoModuleImplV1.extractCipherText(ciphertext1), CryptoModuleImplV1.extractCipherText(ciphertext2)));

        assertEquals(crypto.getMinLength(), ciphertext1.length);
    }

    @Test
    public void testEmptyMessage() throws GeneralSecurityException {
        final String plaintext = "";
        CryptoModuleImplV1 crypto = new CryptoModuleImplV1("password".toCharArray());
        byte[] ciphertext1 = crypto.encrypt(plaintext);
        byte[] ciphertext2 = crypto.encrypt(plaintext);
        assertFalse(Arrays.equals(ciphertext1, ciphertext2));
        assertFalse(Arrays.equals(CryptoModuleImplV1.extractIV(ciphertext1), CryptoModuleImplV1.extractIV(ciphertext2)));
        assertFalse(Arrays.equals(CryptoModuleImplV1.extractCipherText(ciphertext1), CryptoModuleImplV1.extractCipherText(ciphertext2)));

        assertEquals(crypto.getMinLength(), ciphertext1.length);
    }

    @Test(expected = GeneralSecurityException.class)
    public void testWrongPassword() throws GeneralSecurityException {
        String plaintext = "1234";
        CryptoModuleImplV1 crypto1 = new CryptoModuleImplV1("password".toCharArray());
        byte[] ciphertext = crypto1.encrypt(plaintext);

        CryptoModuleImplV1 crypto2 = new CryptoModuleImplV1("badpassword".toCharArray());
        crypto2.decrypt(ciphertext);
    }

    @Test(expected = GeneralSecurityException.class)
    public void testIntegrity() throws GeneralSecurityException {
        String plaintext = "1234";
        CryptoModuleImplV1 crypto = new CryptoModuleImplV1("password".toCharArray());
        byte[] ciphertext = crypto.encrypt(plaintext);
        ciphertext[CryptoModuleImplV1.BLOCK_LENGTH_BYTES + 2] = 0;
        crypto.decrypt(ciphertext);
    }
}