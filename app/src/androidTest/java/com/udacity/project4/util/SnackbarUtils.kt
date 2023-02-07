package com.udacity.project4.util

import android.os.SystemClock
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback.DismissEvent

object SnackbarUtils {
    interface TransientBottomBarAction {
        @Throws(Throwable::class)
        fun perform()
    }

    private class TransientBottomBarCallback<B : BaseTransientBottomBar<B>?> :
        BaseCallback<B>() {
        var shown = false
        var dismissed = false
        override fun onShown(transientBottomBar: B) {
            shown = true
        }

        override fun onDismissed(transientBottomBar: B, @DismissEvent event: Int) {
            dismissed = true
        }
    }

    private val SLEEP_MILLIS = 250

    /**
     * Helper method that shows that specified [Snackbar] and waits until it has been fully
     * shown.
     */
    fun <B : BaseTransientBottomBar<B>?> showTransientBottomBarAndWaitUntilFullyShown(
        transientBottomBar: B
    ) {
        if (transientBottomBar!!.isShown) {
            return
        }
        val callback = TransientBottomBarCallback<B>()
        transientBottomBar.addCallback(callback)
        transientBottomBar.show()
        waitForCallbackShown(callback)
    }

    /** Helper method that waits until the given bar has been fully shown.  */
    fun <B : BaseTransientBottomBar<B>?> waitUntilFullyShown(
        transientBottomBar: B
    ) {
        if (transientBottomBar!!.isShown) {
            return
        }
        val callback = TransientBottomBarCallback<B>()
        transientBottomBar.addCallback(callback)
        waitForCallbackShown(callback)
    }

    /**
     * Helper method that dismissed that specified [Snackbar] and waits until it has been fully
     * dismissed.
     */
    @Throws(Throwable::class)
    fun <B : BaseTransientBottomBar<B>?> dismissTransientBottomBarAndWaitUntilFullyDismissed(
        transientBottomBar: B
    ) {
        performActionAndWaitUntilFullyDismissed(
            transientBottomBar,
            object : TransientBottomBarAction {
                override fun perform() {
                    transientBottomBar!!.dismiss()
                }
            })
    }

    /**
     * Helper method that dismissed that specified [Snackbar] and waits until it has been fully
     * dismissed.
     */
    @Throws(Throwable::class)
    fun <B : BaseTransientBottomBar<B>?> performActionAndWaitUntilFullyDismissed(
        transientBottomBar: B, action: TransientBottomBarAction
    ) {
        if (!transientBottomBar!!.isShown) {
            return
        }
        val callback = TransientBottomBarCallback<B>()
        transientBottomBar.addCallback(callback)
        action.perform()
        waitForCallbackDismissed(callback)
    }

    /** Helper method that waits until the given bar has been fully dismissed.  */
    fun <B : BaseTransientBottomBar<B>?> waitUntilFullyDismissed(
        transientBottomBar: B
    ) {
        if (!transientBottomBar!!.isShown) {
            return
        }
        val callback = TransientBottomBarCallback<B>()
        transientBottomBar.addCallback(callback)
        waitForCallbackDismissed(callback)
    }

    private fun <B : BaseTransientBottomBar<B>?> waitForCallbackShown(
        transientBottomBarCallback: TransientBottomBarCallback<B>
    ) {
        waitForCallback(transientBottomBarCallback, true)
    }

    private fun <B : BaseTransientBottomBar<B>?> waitForCallbackDismissed(
        transientBottomBarCallback: TransientBottomBarCallback<B>
    ) {
        waitForCallback(transientBottomBarCallback, false)
    }

    private fun <B : BaseTransientBottomBar<B>?> waitForCallback(
        transientBottomBarCallback: TransientBottomBarCallback<B>, waitForShown: Boolean
    ) {
        while (waitForShown && !transientBottomBarCallback.shown
            || !waitForShown && !transientBottomBarCallback.dismissed
        ) {
            SystemClock.sleep(SLEEP_MILLIS.toLong())
        }
        SystemClock.sleep(SLEEP_MILLIS.toLong())
    }

}