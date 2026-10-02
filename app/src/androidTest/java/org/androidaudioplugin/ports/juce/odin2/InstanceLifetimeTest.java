package org.androidaudioplugin.ports.juce.odin2;

import android.content.Context;
import android.content.Intent;
import android.os.Debug;
import android.os.SharedMemory;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ServiceTestRule;
import org.androidaudioplugin.AudioPluginInterface;
import org.androidaudioplugin.AudioPluginInterfaceCallback;
import org.androidaudioplugin.AudioPluginExtensionCallback;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;

/** Exercises the real AAP factory and release path in one service process. */
public class InstanceLifetimeTest {
    @Rule public final ServiceTestRule service = new ServiceTestRule();

    private static ParcelFileDescriptor descriptor(SharedMemory memory) {
        Parcel parcel = Parcel.obtain();
        try {
            memory.writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            return parcel.readFileDescriptor();
        } finally {
            parcel.recycle();
        }
    }

    private static void completeCreation(AudioPluginInterface plugin, int id) throws Exception {
        // The V4 service requires the host to supply extension serialization buffers.
        for (String extension : new String[]{"midi2/v3", "parameters/v4", "presets/v4",
                "state/v4", "gui/v4", "urid/v3"}) {
            try (SharedMemory memory = SharedMemory.create("odin-extension", 1024 * 1024);
                 ParcelFileDescriptor fd = descriptor(memory)) {
                plugin.addExtension(id, "urn://androidaudioplugin.org/extensions/" + extension,
                        fd, 1024 * 1024);
            }
        }
        plugin.endCreate(id);
    }

    @Test public void releasedProcessorsDoNotAccumulate() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent("org.androidaudioplugin.AudioPluginService.V4")
                .setClassName(context, "org.androidaudioplugin.AudioPluginService");
        AudioPluginInterface plugin = AudioPluginInterface.Stub.asInterface(service.bindService(intent));
        plugin.setCallback(new AudioPluginInterfaceCallback.Stub() {
            public void hostExtension(int id, String uri, int opcode, int request,
                                      AudioPluginExtensionCallback callback) {}
            public void requestProcess(int id) {}
        });
        long warmedUp = 0;
        for (int i = 1; i <= 100; i++) {
            int id = plugin.beginCreate("juceaap:4f44494e");
            try {
                completeCreation(plugin, id);
                assertTrue(plugin.isPluginAlive(id));
            } finally {
                plugin.destroy(id);
            }
            assertFalse(plugin.isPluginAlive(id));
            if (i == 10) warmedUp = Debug.getNativeHeapAllocatedSize();
            if (i % 10 == 0)
                Log.i("OdinLifetime", "released=" + i + " nativeBytes=" + Debug.getNativeHeapAllocatedSize());
        }
        long growth = Debug.getNativeHeapAllocatedSize() - warmedUp;
        assertTrue("Native heap grew by " + growth + " bytes after warmup", growth < 8 * 1024 * 1024);
    }

    @Test public void multipleLiveInstancesCanBeReleased() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent("org.androidaudioplugin.AudioPluginService.V4")
                .setClassName(context, "org.androidaudioplugin.AudioPluginService");
        AudioPluginInterface plugin = AudioPluginInterface.Stub.asInterface(service.bindService(intent));
        int[] ids = new int[48];
        int created = 0;
        try {
            for (int i = 0; i < ids.length; i++) {
                ids[i] = plugin.beginCreate("juceaap:4f44494e");
                created++;
                completeCreation(plugin, ids[i]);
                assertTrue(plugin.isPluginAlive(ids[i]));
            }
            Log.i("OdinLifetime", "live=48 nativeBytes=" + Debug.getNativeHeapAllocatedSize());
        } finally {
            for (int i = created - 1; i >= 0; i--)
                plugin.destroy(ids[i]);
        }
        for (int id : ids) assertFalse(plugin.isPluginAlive(id));
        Log.i("OdinLifetime", "releasedAll=48 nativeBytes=" + Debug.getNativeHeapAllocatedSize());
    }
}
