package biz.ftsdesign.paranoiddiary.data;

import android.util.Log;

import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Keeps the password in memory for as long as the class is loaded
 */
public class TransientPasswordStorage {
    private static final long TIMEOUT_MS = 600000;
    private static final Set<PasswordListener> listeners = new HashSet<>();
    private static transient char[] pass;
    private static final Timer timer = new Timer("PassExpiryTimer");
    private static TimerTask clearPasswordTask = null;

    /**
     *
     * @param password Password to set
     * @return true if the password was set, false otherwise
     */
    public static synchronized boolean setPassword(@Nullable String password) {
        if (password == null || password.trim().isEmpty()) {
            Log.w(TransientPasswordStorage.class.getSimpleName(), "Password must be not empty, or use clear() to clear it");
            return false;
        }
        pass = password.toCharArray();
        Log.i(TransientPasswordStorage.class.getSimpleName(), "Password is set");
        updateTime();
        fireOnPasswordSet();
        return true;
    }

    private static void updateTime() {
        if (clearPasswordTask != null) {
            clearPasswordTask.cancel();
        }
        clearPasswordTask = new TimerTask() {
            @Override
            public void run() {
                Log.i(TransientPasswordStorage.class.getCanonicalName(), "Clearing password on timeout");
                clear();
            }
        };
        timer.schedule(clearPasswordTask, TIMEOUT_MS);
    }

    public static synchronized boolean isSet() {
        updateTime();
        return pass != null;
    }

    @Nullable
    static synchronized char[] getPassword() {
        updateTime();
        return pass;
    }

    public static synchronized void clear() {
        if (pass != null) {
            pass = null;
            Log.i(TransientPasswordStorage.class.getSimpleName(), "Password cleared");
            fireOnAfterPasswordCleared();
        }
    }

    private static void fireOnAfterPasswordCleared() {
        for (PasswordListener listener : listeners) {
            try {
                listener.onAfterPasswordCleared();
            } catch (Exception e) {
                Log.e(TransientPasswordStorage.class.getCanonicalName(), "onAfterPasswordCleared", e);
            }
        }
    }

    private static void fireOnPasswordSet() {
        for (PasswordListener listener : listeners) {
            try {
                listener.onPasswordSet();
            } catch (Exception e) {
                Log.e(TransientPasswordStorage.class.getCanonicalName(), "onPasswordSet", e);
            }
        }
    }

    public static synchronized void addListener(PasswordListener listener) {
        updateTime();
        listeners.add(listener);
    }

    public static synchronized void removeListener(PasswordListener listener) {
        updateTime();
        listeners.remove(listener);
    }

    public static boolean isPasswordCorrect(String passwordToCheck) {
        return isSet() && Arrays.equals(pass, passwordToCheck.toCharArray());
    }
}
