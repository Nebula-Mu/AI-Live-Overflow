package com.nebula.deskpet

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.app.AppOpsManager
import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nebula.deskpet.service.OverlayService

/**
 * 只干两件事：拿悬浮窗权限，开关服务。
 * 界面用代码写，省掉 layout xml。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var usageStatusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(64, 64, 64, 64)
            setBackgroundColor(Color.parseColor("#FFF0F4"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        statusText = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.parseColor("#88566B"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }
        root.addView(statusText)

        usageStatusText = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.parseColor("#88566B"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 48)
        }
        root.addView(usageStatusText)

        root.addView(Button(this).apply {
            text = getString(R.string.grant_overlay)
            setOnClickListener { requestOverlayPermission() }
        })

        root.addView(Button(this).apply {
            text = "授权使用情况访问"
            setOnClickListener { requestUsageAccess() }
        })

        root.addView(Button(this).apply {
            text = getString(R.string.start_pet)
            setOnClickListener { startPet() }
        })

        root.addView(Button(this).apply {
            text = getString(R.string.stop_pet)
            setOnClickListener {
                stopService(Intent(this@MainActivity, OverlayService::class.java))
            }
        })

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        statusText.text = if (canDrawOverlays()) "悬浮窗已授权" else "还没授权悬浮窗"
        usageStatusText.text = if (hasUsageAccess()) "使用情况访问已授权" else "还没授权使用情况访问（换装功能需要）"
    }

    private fun canDrawOverlays(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)

    private fun hasUsageAccess(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun requestOverlayPermission() {
        if (canDrawOverlays()) {
            Toast.makeText(this, "已经给过了", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
        )
    }

    private fun requestUsageAccess() {
        if (hasUsageAccess()) {
            Toast.makeText(this, "已经给过了", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    private fun startPet() {
        if (!canDrawOverlays()) {
            Toast.makeText(this, "先授权悬浮窗", Toast.LENGTH_SHORT).show()
            return
        }
        if (!hasUsageAccess()) {
            Toast.makeText(this, "建议授权使用情况访问，否则换装功能无法工作", Toast.LENGTH_LONG).show()
        }
        startForegroundService(Intent(this, OverlayService::class.java))
    }
}