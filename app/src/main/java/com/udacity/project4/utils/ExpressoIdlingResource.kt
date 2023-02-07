package com.udacity.project4.utils

import androidx.test.espresso.idling.CountingIdlingResource

object ExpressoIdlingResource {

    private const val RESOURCE = "GLOBAL"

    @JvmField
    val countingIdlingResource = CountingIdlingResource(RESOURCE)

    fun increment() {
        countingIdlingResource.increment()
    }

    fun decrement() {
        if (!countingIdlingResource.isIdleNow) {
            countingIdlingResource.decrement()
        }
    }

}
    inline fun <T> wrapEspressoIdlingResource(function: () -> T): T {
        ExpressoIdlingResource.increment()
        return try {
            function()
        } finally {
            ExpressoIdlingResource.decrement()
        }
    }
