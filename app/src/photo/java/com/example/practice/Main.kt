package com.example.practice

import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : PracticeActivity() {
    private val prefs by lazy { getSharedPreferences("photo", MODE_PRIVATE) }
    private var selected: Uri? = null
    private var loading: Job? = null
    private lateinit var preview: ImageView
    private lateinit var status: TextView
    private lateinit var clear: Button

    // 提前注册回调，Activity 重建后也能接收系统选择器返回的 URI。
    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) select(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = page("照片预览")
        val card = layout.card()
        status = card.text("请选择一张图片").apply {
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        preview = ImageView(this).apply {
            contentDescription = "选中的图片"
            scaleType = ImageView.ScaleType.FIT_CENTER
            visibility = View.GONE
        }
        card.addView(preview, LinearLayout.LayoutParams(MATCH_PARENT, dp(260)).apply {
            topMargin = dp(12)
        })
        layout.button("选择图片") {
            try {
                picker.launch(arrayOf("image/*"))
            } catch (_: ActivityNotFoundException) {
                status.text = "设备上没有可用的文件选择器"
            }
        }
        clear = layout.button("移除预览") {
            loading?.cancel()
            forgetSavedUri()
            selected = null
            preview.setImageDrawable(null)
            preview.visibility = View.GONE
            clear.isEnabled = false
            status.text = "请选择一张图片"
        }.apply { isEnabled = false }

        val previous = savedInstanceState?.getString(KEY_URI) ?: prefs.getString(KEY_URI, null)
        if (previous != null) showPhoto(Uri.parse(previous))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_URI, selected?.toString())
        super.onSaveInstanceState(outState)
    }

    private fun select(uri: Uri) {
        val previous = prefs.getString(KEY_URI, null)
        // 保存 URI 和长期读取授权即可。系统只授予用户选中的文档的读取权限。
        val canPersist = try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            true
        } catch (_: SecurityException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
        prefs.edit().putString(KEY_URI, if (canPersist) uri.toString() else null).apply()
        if (previous != null && previous != uri.toString()) release(Uri.parse(previous))
        showPhoto(uri)
    }

    private fun forgetSavedUri() {
        val previous = prefs.getString(KEY_URI, null)
        prefs.edit().remove(KEY_URI).apply()
        if (previous != null) release(Uri.parse(previous))
    }

    private fun release(uri: Uri) {
        try {
            contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            // 提供器可能已撤销权限，例如原文件被删除。
        }
    }

    private fun showPhoto(uri: Uri) {
        loading?.cancel()
        selected = uri
        clear.isEnabled = true
        preview.setImageDrawable(null)
        preview.visibility = View.GONE
        status.text = "正在打开图片…"
        val resolver = contentResolver
        loading = lifecycleScope.launch {
            // 大图解码在 IO 线程进行。切换图片时取消旧任务，界面只接收当前结果。
            val bitmap = withContext(Dispatchers.IO) { readPreview(resolver, uri) }
            if (bitmap == null) {
                status.text = "无法打开图片，请重新选择"
            } else {
                preview.setImageBitmap(bitmap)
                preview.visibility = View.VISIBLE
                status.text = if (prefs.getString(KEY_URI, null) == uri.toString()) "已保存选择"
                    else "本次可以预览，下次打开需重新选择"
            }
        }
    }

    private companion object {
        const val KEY_URI = "image_uri"
    }
}

private fun readPreview(resolver: ContentResolver, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
                decoder.setTargetSampleSize(sampleSize(info.size.width, info.size.height))
            }
        } else {
            // API 24–27 先读取宽高，再打开同一 URI 做降采样解码。
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
            }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        }
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}

private fun sampleSize(width: Int, height: Int): Int {
    // 限制预览尺寸，避免把高分辨率原图一次完整载入内存。
    val longest = maxOf(width, height)
    var sample = 1
    while (longest / sample > 1600) sample *= 2
    return sample
}
