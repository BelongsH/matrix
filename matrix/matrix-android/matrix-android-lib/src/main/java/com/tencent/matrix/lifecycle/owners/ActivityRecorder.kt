package com.tencent.matrix.lifecycle.owners

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.tencent.matrix.lifecycle.StatefulOwner
import com.tencent.matrix.lifecycle.EmptyActivityLifecycleCallbacks
import com.tencent.matrix.util.MatrixLog
import java.util.*

/**
 * Created by Yves on 2021/9/24
 */
object ActivityRecorder : StatefulOwner() {

    private const val TAG = "Matrix.memory.ActivityRecorder"

    private val callbacks = ActivityCallbacks()

    private val activityRecord = WeakHashMap<Activity, Any>()

    private fun WeakHashMap<Activity, Any>.contains(clazz: Class<*>) : Boolean {
        entries.forEach { e ->
            if (clazz == e.key?.javaClass) {
                return true
            }
        }
        return false
    }

    private fun WeakHashMap<Activity, Any>.contains(clazz: String) : Boolean {
        entries.forEach { e ->
            if (clazz == e.key?.javaClass?.name) {
                return true
            }
        }
        return false
    }

    private fun Set<Activity>.contentToString(): String {

        val linkedList = LinkedList<String>()

        this.forEach {
            linkedList.add(it.javaClass.simpleName)
        }
        return "[${this.size}] $linkedList"
    }

    var currentActivity: String = "default"
        private set

    @Volatile
    private var inited = false

    fun init(app: Application) {
        if (inited) {
            return
        }
        inited = true
        app.registerActivityLifecycleCallbacks(callbacks)
    }

    fun sizeExcept(activityNames: Array<String>?) : Int {
        var size = activityRecord.size
        activityNames?.forEach {
            if (activityRecord.contains(it)) {
                size--
            }
        }
        return size
    }

    private fun onStateChanged() {
        if (activityRecord.isEmpty()) {
            turnOff()
        } else {
            turnOn()
        }
    }

    private class ActivityCallbacks : EmptyActivityLifecycleCallbacks() {

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            activityRecord[activity] = Any()
            onStateChanged()
            MatrixLog.d(
                TAG, "[${activity.javaClass.simpleName}] -> ${
                    activityRecord.keys.contentToString()
                }"
            )
        }

        override fun onActivityResumed(activity: Activity) {
            currentActivity = activity.javaClass.simpleName
        }

        override fun onActivityDestroyed(activity: Activity) {
            activityRecord.remove(activity)
            onStateChanged()
            MatrixLog.d(
                TAG, "[${activity.javaClass.simpleName}] <- ${
                    activityRecord.keys.contentToString()
                }"
            )
        }
    }
}