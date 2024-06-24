package com.sd.lib.stream.binder

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.sd.lib.stream.FStream

/**
 * 把流对象和[Activity]绑定，在[Activity]销毁时，取消注册流对象
 */
internal class ActivityStreamBinder(
    stream: FStream,
    target: Activity,
) : StreamBinder<Activity>(stream, target) {

    override fun bindImpl(target: Activity): Boolean {
        if (target.isFinishing) return false
        return registerStream().also { register ->
            if (register) {
                target.application.registerActivityLifecycleCallbacks(_activityCallbacks)
            }
        }
    }

    private val _activityCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        }

        override fun onActivityStarted(activity: Activity) {
        }

        override fun onActivityResumed(activity: Activity) {
        }

        override fun onActivityPaused(activity: Activity) {
        }

        override fun onActivityStopped(activity: Activity) {
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        }

        override fun onActivityDestroyed(activity: Activity) {
            val t = getTarget()
            if (t == null || t === activity) {
                activity.application.unregisterActivityLifecycleCallbacks(this)
                destroy()
            }
        }
    }

    override fun onDestroy(target: Activity) {
        target.application.unregisterActivityLifecycleCallbacks(_activityCallbacks)
    }
}