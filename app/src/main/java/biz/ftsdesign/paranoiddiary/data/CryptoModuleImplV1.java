package biz.ftsdesign.paranoiddiary.data;

import androidx.annotation.NonNull;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 https://developer.android.com/guide/topics/security/cryptography
 Cipher 	AES in either CBC or GCM mode with 256-bit keys (such as AES/GCM/NoPadding)
 MessageDigest 	SHA-2 family (eg, SHA-256)
 Mac 	SHA-2 family HMAC (eg, HMACSHA256)
 Signature 	SHA-2 family with ECDSA (eg, SHA256withECDSA)

 https://gist.github.com/dweymouth/11089238
 https://en.wikipedia.org/wiki/Initialization_vector
 https://security.stackexchange.com/questions/48000/why-would-you-need-a-salt-for-aes-cbs-when-iv-is-already-randomly-generated-and
 https://en.wikipedia.org/wiki/Advanced_Encryption_Standard
 https://en.wikipedia.org/wiki/Block_cipher_mode_of_operation
 */
class CryptoModuleImplV1 implements CryptoModule {
    private static final String CIPHER_SPEC = "AES/CBC/PKCS5PADDING";
    private static final String KEYGEN_SPEC = "PBKDF2WithHmacSHA1";
    private static final String ALGORITHM = "AES";
    /*
    It must be large enough to make generating the key computationally expensive.
     */
    private static final int ITERATIONS = 10000;
    private static final int KEY_SIZE_BITS = 256;
    static final int BLOCK_LENGTH_BYTES = 16;
    private static final int IV_LENGTH_BYTES = BLOCK_LENGTH_BYTES;
    private final SecretKey secretKey;

    CryptoModuleImplV1(@NonNull char[] password) throws InvalidKeySpecException, NoSuchAlgorithmException {
        // This is to ensure that salt is derived from the password, but is different for different passwords
        byte[] salt = longToBytes(new String(password).hashCode());

        // The key for the same password is always the same. We create it only once.
        secretKey = keygen(password, salt);
    }

    private static @NonNull byte[] longToBytes(long l) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte)(l & 0xFF);
            l >>= 8;
        }
        return result;
    }

    private @NonNull SecretKey keygen(@NonNull final char[] password, @NonNull final byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        long t1 = System.currentTimeMillis();
        final KeySpec keySpec = new PBEKeySpec(password, salt, ITERATIONS, KEY_SIZE_BITS);
        final SecretKeyFactory factory = SecretKeyFactory.getInstance(KEYGEN_SPEC);
        final SecretKey secretKey = new SecretKeySpec(factory.generateSecret(keySpec).getEncoded(), ALGORITHM);
        t1 = System.currentTimeMillis() - t1;
        System.out.println("Key generated in " + t1 + " ms");
        return secretKey;
    }

    /*
    The output consists of:
    - IV
    - Ciphertext
     */
    @Override
    @NonNull
    public byte[] encrypt(@NonNull String plaintext) throws GeneralSecurityException {
        final byte[] plaintextBytes = plaintext.getBytes();
        return encrypt(plaintextBytes);
    }

    @Override
    @NonNull
    public byte[] encrypt(@NonNull byte[] plaintextBytes) throws GeneralSecurityException {
        final Cipher cipher = Cipher.getInstance(CIPHER_SPEC);

        /*
         * <p>If this cipher (including its underlying feedback or padding scheme)
         * requires any random bytes (e.g., for parameter generation), it will get
         * them using the {@link java.security.SecureRandom}
         * implementation of the highest-priority
         * installed provider as the source of randomness.
         * (If none of the installed providers supply an implementation of
         * SecureRandom, a system-provided source of randomness will be used.)
         */
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        /*
        You don't need to keep the IV secret, but it must be random and unique.
        Here we assume that cipher gives us randomness and uniqueness.
         */
        final byte[] iv = cipher.getIV();
        final byte[] ciphertext = cipher.doFinal(plaintextBytes);
        return DataUtils.assemble(iv, ciphertext);
    }

    static byte[] extractIV(@NonNull final byte[] bytes) {
        final byte[] iv = new byte[IV_LENGTH_BYTES];
        System.arraycopy(bytes, 0, iv, 0, iv.length);
        return iv;
    }

    static byte[] extractCipherText(@NonNull final byte[] bytes) {
        final byte[] cipherText = new byte[bytes.length - IV_LENGTH_BYTES];
        System.arraycopy(bytes, IV_LENGTH_BYTES, cipherText, 0, cipherText.length);
        return cipherText;
    }

    @Override
    @NonNull
    public byte[] decryptBytes(@NonNull final byte[] in) throws GeneralSecurityException {
        final Cipher cipher = Cipher.getInstance(CIPHER_SPEC);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(extractIV(in)));
        return cipher.doFinal(extractCipherText(in));
    }

    @Override
    @NonNull
    public String decrypt(@NonNull final byte[] in) throws GeneralSecurityException {
        return new String(decryptBytes(in));
    }

    @Override
    public int getMinLength() {
        return IV_LENGTH_BYTES + BLOCK_LENGTH_BYTES;
    }
}
