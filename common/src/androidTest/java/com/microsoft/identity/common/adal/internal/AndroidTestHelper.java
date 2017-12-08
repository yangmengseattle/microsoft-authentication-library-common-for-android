package com.microsoft.identity.common.adal.internal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.support.test.InstrumentationRegistry;
import android.util.Base64;
import android.util.Log;

import com.microsoft.identity.common.adal.internal.net.HttpUrlConnectionFactory;

import junit.framework.Assert;

import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class AndroidTestHelper {

    protected static final int REQUEST_TIME_OUT = 40000; // milliseconds

    private static final String TAG = "AndroidTestHelper";

    private byte[] mTestSignature;

    private String mTestTag;

    @SuppressLint("PackageManagerGetSignatures")
    public void setUp() throws Exception {
        getInstrumentation().getTargetContext().getCacheDir();
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext().getCacheDir().getPath());

        // ADAL is set to this signature for now
        final Context context = getInstrumentation().getContext();
        PackageInfo info = context.getPackageManager()
                .getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
        for (Signature signature : info.signatures) {
            mTestSignature = signature.toByteArray();
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(mTestSignature);
            mTestTag = Base64.encodeToString(md.digest(), Base64.DEFAULT);
            break;
        }
        AuthenticationSettings.INSTANCE.setBrokerSignature(mTestTag);
        AuthenticationSettings.INSTANCE
                .setBrokerPackageName(AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);
        Log.d(TAG, "mTestSignature is set");
    }

    public void tearDown() throws Exception {
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(null);
    }

    public void assertThrowsException(final Class<? extends Exception> expected, String hasMessage,
                                      final ThrowableRunnable testCode) {
        try {
            testCode.run();
            Assert.fail("This is expecting an exception, but it was not thrown.");
        } catch (final Throwable result) {
            if (!expected.isInstance(result)) {
                Assert.fail("Exception was not correct");
            }

            if (hasMessage != null && !hasMessage.isEmpty()) {
                assertTrue("Message has the text " + result.getMessage(),
                        (result.getMessage().toLowerCase(Locale.US)
                                .contains(hasMessage.toLowerCase(Locale.US))));
            }
        }
    }

    public void assertThrowsException(final Class<? extends Exception> expected, String hasMessage,
                                      final Runnable testCode) {
        try {
            testCode.run();
            Assert.fail("This is expecting an exception, but it was not thrown.");
        } catch (final Throwable result) {
            if (!expected.isInstance(result)) {
                Assert.fail("Exception was not correct");
            }

            if (hasMessage != null && !hasMessage.isEmpty()) {
                assertTrue("Message has the text", (result.getMessage().toLowerCase(Locale.US).contains(hasMessage)));
            }
        }
    }

    /**
     * just run tests and wait until finished
     *
     * @param signal
     * @param testCode
     * @param runOnUI
     */
    public void testAsyncNoExceptionUIOption(final CountDownLatch signal, final Runnable testCode, boolean runOnUI) {

        Log.d(getClass().getName(), "thread:" + android.os.Process.myTid());

        try {
            if (runOnUI) {
                // run on UI thread to create async object at UI thread.
                // Background
                // work will happen in another thread.
                InstrumentationRegistry.getInstrumentation().runOnMainSync(testCode);
            } else {
                testCode.run();
            }
        } catch (Throwable ex) {
            Log.e(getClass().getName(), ex.getMessage());
            assertFalse("not expected:" + ex.getMessage(), true);
            signal.countDown();
        }

        try {
            signal.await(REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            assertFalse("Timeout " + getClass().getName(), true);
        }
    }

    public void testMultiThread(int activeThreads, final CountDownLatch signal, final Runnable runnable) {

        Log.d(getClass().getName(), "thread:" + android.os.Process.myTid());

        Thread[] threads = new Thread[activeThreads];

        for (int i = 0; i < activeThreads; i++) {
            Log.d(getClass().getName(), "Run shared cache test for thread:" + i);
            threads[i] = new Thread(runnable);
            threads[i].start();
        }

        try {
            signal.await(REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            assertFalse("Timeout " + getClass().getName(), true);
        }
    }

    public interface ThrowableRunnable {
        void run() throws Exception;
    }
}

