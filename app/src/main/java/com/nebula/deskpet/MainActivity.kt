package com.nebula.deskpet

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nebula.deskpet.service.OverlayService

/**
 * 只干三件事：拿悬浮窗权限，拿使用情况访问权限，开关服务。界面用代码写，省掉 layout xml。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var usageStatusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#FFF0F4"))
            setPadding(dpToPx(64), dpToPx(64), dpToPx(64), dpToPx(64))
        }

        statusText = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.parseColor("#88566B"))
            gravity = Gravity.CENTER
        }

        usageStatusText = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.parseColor("#88566B"))
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(16), 0, 0)
        }

        val btnOverlay = Button(this).apply {
            text = getString(R.string.grant_overlay)
            setOnClickListener { requestOverlayPermission() }
        }

        val btnUsage = Button(this).apply {
            text = "授权使用情况访问"
            setOnClickListener { requestUsageStatsPermission() }
        }

        val btnStart = Button(this).apply {
            text = getString(R.string.start_pet)
            setOnClickListener { startPet() }
        }

        val btnStop = Button(this).apply {
            text = getString(R.string.stop_pet)
            setOnClickListener { stopPet() }
        }

        container.addView(statusText)
        container.addView(usageStatusText)
        container.addView(btnOverlay)
        container.addView(btnUsage)
        container.addView(btnStart)
        container.addView(btnStop)

        setContentView(container)
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        statusText.text = if (Settings.canDrawOverlays(this)) {
            "悬浮窗已授权"
        } else {
            "还没授权悬浮窗"
        }

        usageStatusText.text = if (hasUsageStatsPermission()) {
            "使用情况访问已授权\n(小狗可以换装了)"
        } else {
            "还没授权使用情况访问\n(换装功能需要)"
        }
    }

    private fun requestOverlayPermission() {
        if (Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "已经给过了", Toast.LENGTH_SHORT).show()
        } else {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun requestUsageStatsPermission() {
        if (hasUsageStatsPermission()) {
            Toast.makeText(this, "已经给过了", Toast.LENGTH_SHORT).show()
        } else {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "找到本应用并开启", Toast.LENGTH_LONG).show()
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun startPet() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "先授权悬浮窗", Toast.LENGTH_SHORT).show()
            return
        }
        if (!hasUsageStatsPermission()) {
            Toast.makeText(this, "换装功能需要使用情况访问权限\n不授权也能用，但不会换装", Toast.LENGTH_LONG).show()
        }
        startForegroundService(Intent(this, OverlayService::class.java))
    }

    private fun stopPet() {
        stopService(Intent(this, OverlayService::class.java))
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}