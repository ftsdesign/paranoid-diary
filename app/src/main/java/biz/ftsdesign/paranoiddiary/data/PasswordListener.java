package biz.ftsdesign.paranoiddiary.data;

public interface PasswordListener {
    /**
     * This is where listeners clear/disable any sensitive content.
     */
    void onAfterPasswordCleared();

    void onPasswordSet();
}
