package org.koreader.launcher

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.StrictMode
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class MainApp : Application() {
    companion object {
        const val NAME = BuildConfig.APP_NAME
        const val FLAVOR = BuildConfig.FLAVOR_CHANNEL
        const val OTA_UPDATES = BuildConfig.IN_APP_UPDATES
        const val RUNTIME_CHANGES = BuildConfig.SUPPORTS_RUNTIME_CHANGES
        const val PROVIDER = "${BuildConfig.APPLICATION_ID}.provider"
        val is_debug = BuildConfig.DEBUG

        // internal path for app files
        lateinit var assets_path: String
            private set

        // internal path for crash logs, used by crash reporter
        lateinit var crash_report_path: String
            private set

        // external path
        lateinit var storage_path: String
            private set

        // app dir in external path
        lateinit var app_storage_path: String
            private set

        private var targetSdk = 0

        // logcat to crash.log - requested by the user, keep in external app path
        fun dumpLogcat() {
            val path = String.format("%s%s%s", app_storage_path, File.separator, "crash.log")
            writeLogToFile(path)
        }

        // crash report
        fun crashReport(context: Context, reason: String? = null) {
            writeLogToFile(crash_report_path, true)
            val reportIntent = Intent(context, CrashReportActivity::class.java)
            reportIntent.putExtra("title", "$NAME crashed")
            reportIntent.putExtra("reason", reason ?: "")
            reportIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_TASK_ON_HOME or
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                Intent.FLAG_ACTIVITY_NO_HISTORY
            context.startActivity(reportIntent)
        }

        fun isAtLeastApi(version: Int, runtimeOnly: Boolean = false): Boolean {
            return if (runtimeOnly) {
                (Build.VERSION.SDK_INT >= version)
            } else {
                ((Build.VERSION.SDK_INT >= version) and (targetSdk >= version))
            }
        }

        private fun getLogBuffer(cleanAfterDump: Boolean = false): BufferedReader {
            val proc = Runtime.getRuntime().exec("logcat -d -v time")
            if (cleanAfterDump) Runtime.getRuntime().exec("logcat -c")
            return BufferedReader(InputStreamReader(proc.inputStream))
        }

        private fun writeLogToFile(path: String, cleanAfterDump: Boolean = false) {
            File(path).let {
                if (it.exists()) it.delete()
                try {
                    it.printWriter().use { log ->
                        val buffer = getLogBuffer(cleanAfterDump)
                        while (true) {
                            buffer.readLine()?.let { line ->
                                log.append("$line\n")
                            } ?: break
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onCreate() {
        super.onCreate()
        assets_path = filesDir.absolutePath
        storage_path = Environment.getExternalStorageDirectory().absolutePath
        app_storage_path = String.format("%s%s%s", storage_path, File.separator, NAME.lowercase())
        crash_report_path = String.format("%s%s%s", cacheDir, File.separator, "crash.log")
        targetSdk = applicationContext.applicationInfo.targetSdkVersion

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            throwable.printStackTrace()
            crashReport(applicationContext,
                "Uncaught exception in thread #${thread.id} (${thread.name})")
            kotlin.system.exitProcess(1)
        }

        if (is_debug) {
            val threadPolicy = StrictMode.ThreadPolicy.Builder().detectCustomSlowCalls()
                .penaltyLog().build()

            val vmPolicy = StrictMode.VmPolicy.Builder().detectActivityLeaks()
                .detectLeakedClosableObjects()
                .penaltyLog().build()

            StrictMode.setThreadPolicy(threadPolicy)
            StrictMode.setVmPolicy(vmPolicy)
        }
    }
}
