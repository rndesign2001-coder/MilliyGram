/*
 * MilliyGram — norasmiy Telegram klienti.
 * GNU GPL v2 yoki keyingi versiya asosida tarqatiladi.
 */

package org.telegram.ui;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.MgConfig;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.INavigationLayout;
import org.telegram.ui.Components.BulletinFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 📳 Silkitib yashirish: telefon keskin silkitilsa, yashirin bo'lim va qulflangan/yashirin
 * chatlar darhol yopiladi, yashirilgan akkauntdan boshqa akkauntga o'tiladi.
 */
public class MgShake implements SensorEventListener {

    private static MgShake instance;
    private SensorManager sensorManager;
    private long lastShake;
    private int shakeCount;
    private long lastTrigger;

    public static void start(Context context) {
        if (!MgConfig.isShakeToHide()) {
            stop();
            return;
        }
        try {
            if (instance == null) {
                instance = new MgShake();
            }
            if (instance.sensorManager == null) {
                instance.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
                Sensor sensor = instance.sensorManager != null ? instance.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) : null;
                if (sensor != null) {
                    instance.sensorManager.registerListener(instance, sensor, SensorManager.SENSOR_DELAY_UI);
                } else {
                    instance.sensorManager = null;
                }
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    public static void stop() {
        try {
            if (instance != null && instance.sensorManager != null) {
                instance.sensorManager.unregisterListener(instance);
                instance.sensorManager = null;
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0] / SensorManager.GRAVITY_EARTH;
        float y = event.values[1] / SensorManager.GRAVITY_EARTH;
        float z = event.values[2] / SensorManager.GRAVITY_EARTH;
        double g = Math.sqrt(x * x + y * y + z * z);
        if (g < 2.6) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        if (now - lastShake > 900) {
            shakeCount = 0;
        }
        if (now - lastShake < 120) {
            return;
        }
        lastShake = now;
        shakeCount++;
        if (shakeCount >= 3 && now - lastTrigger > 2500) {
            lastTrigger = now;
            shakeCount = 0;
            panic();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private static boolean isSensitive(BaseFragment f) {
        if (f instanceof MgHiddenActivity || f instanceof MgHiddenSettingsActivity || f instanceof MgChatLockSettingsActivity) {
            return true;
        }
        if (f instanceof ChatActivity) {
            long did = ((ChatActivity) f).getDialogId();
            int acc = f.getCurrentAccount();
            return MgConfig.isDialogHidden(acc, did) || MgConfig.isDialogLocked(acc, did);
        }
        return false;
    }

    private static void panic() {
        LaunchActivity la = LaunchActivity.instance;
        if (la == null) {
            return;
        }
        try {
            INavigationLayout layout = la.getActionBarLayout();
            boolean did = false;
            if (layout != null) {
                List<BaseFragment> stack = new ArrayList<>(layout.getFragmentStack());
                boolean sensitive = false;
                for (BaseFragment f : stack) {
                    if (isSensitive(f)) {
                        sensitive = true;
                        break;
                    }
                }
                if (sensitive && stack.size() > 1) {
                    for (int i = stack.size() - 2; i >= 1; i--) {
                        layout.removeFragmentFromStack(stack.get(i));
                    }
                    layout.closeLastFragment(false);
                    did = true;
                }
            }
            if (MgConfig.isAccountHidden(UserConfig.selectedAccount)) {
                MgAccountMenu.switchToVisibleAccount(UserConfig.selectedAccount);
                did = true;
            }
            if (did && layout != null && layout.getLastFragment() != null) {
                BulletinFactory.of(layout.getLastFragment()).createSimpleBulletin(R.raw.contact_check, "📳 Hammasi yashirildi").show();
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }
}
