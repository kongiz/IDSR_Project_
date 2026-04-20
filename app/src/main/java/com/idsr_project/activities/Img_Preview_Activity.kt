package com.idsr_project.activities

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.github.chrisbanes.photoview.PhotoView
import com.idsr_project.databinding.ActivityImgPreviewBinding
import com.idsr_project.utils.SessionManager
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.OutputStream
import kotlin.math.abs

class Img_Preview_Activity : AppCompatActivity() {
    private lateinit var binding: ActivityImgPreviewBinding

    private val client = OkHttpClient.Builder().build()
    private var images = listOf<String>()
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityImgPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        images = intent.getStringArrayListExtra("imageList") ?: emptyList()

        if (images.isEmpty()) {
            Toast.makeText(this, "No images to preview", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupSwipe()
        showImage(currentIndex)

        binding.btnClose.setOnClickListener { finish() }
        binding.btnDownload.setOnClickListener { downloadImage(images[currentIndex]) }
        binding.btnShare.setOnClickListener { shareImage(images[currentIndex]) }
    }

    private fun setupSwipe() {
        binding.fullImage.setOnSingleFlingListener { e1, e2, velocityX, _ ->
            val diffX = e2.x - e1.x
            if (abs(diffX) > 100 && abs(velocityX) > 100) {
                if (diffX < 0) {
                    if (currentIndex < images.size - 1) {
                        currentIndex++
                        showImage(currentIndex)
                    } else {
                        Toast.makeText(this, "Last image", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    if (currentIndex > 0) {
                        currentIndex--
                        showImage(currentIndex)
                    } else {
                        Toast.makeText(this, "First image", Toast.LENGTH_SHORT).show()
                    }
                }
                true
            } else {
                false
            }
        }
    }

    private fun showImage(index: Int) {
        val url   = images[index]
        val token = SessionManager.getAccessToken(this)

        binding.progressLoading.visibility = View.VISIBLE

        val glideUrl = GlideUrl(
            url,
            LazyHeaders.Builder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        )

        binding.fullImage.animate().alpha(0f).setDuration(150).withEndAction {
            Glide.with(this)
                .load(glideUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<android.graphics.drawable.Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.progressLoading.visibility = View.GONE
                        runOnUiThread {
                            Toast.makeText(this@Img_Preview_Activity, "Failed to load image", Toast.LENGTH_SHORT).show()
                        }
                        return false
                    }

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable,
                        model: Any,
                        target: Target<android.graphics.drawable.Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.progressLoading.visibility = View.GONE
                        binding.fullImage.animate().alpha(1f).setDuration(200).start()
                        return false
                    }
                })
                .into(binding.fullImage)
        }.start()

        if (images.size > 1) {
            Toast.makeText(this, "${index + 1} / ${images.size}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun downloadImage(url: String) {
        binding.progressLoading.visibility = View.VISIBLE
        val token = SessionManager.getAccessToken(this)

        Thread {
            try {
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                val bytes = client.newCall(request).execute().body?.bytes()

                if (bytes == null) {
                    runOnUiThread {
                        binding.progressLoading.visibility = View.GONE
                        Toast.makeText(this, "Download failed!", Toast.LENGTH_LONG).show()
                    }
                    return@Thread
                }

                val filename = "lab_result_${System.currentTimeMillis()}.jpg"
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/IDSRSaved")
                }

                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                val fos: OutputStream? = uri?.let { contentResolver.openOutputStream(it) }
                fos?.write(bytes)
                fos?.close()

                runOnUiThread {
                    binding.progressLoading.visibility = View.GONE
                    Toast.makeText(this, "Image saved to gallery!", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    binding.progressLoading.visibility = View.GONE
                    Toast.makeText(this, "Save failed!", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun shareImage(url: String) {
        binding.progressLoading.visibility = View.VISIBLE
        val token = SessionManager.getAccessToken(this)

        Thread {
            try {
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                val bytes  = client.newCall(request).execute().body?.bytes() ?: return@Thread
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                val fileName = "shared_lab_${System.currentTimeMillis()}.jpg"
                val values   = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                }

                val uri    = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                val stream = uri?.let { contentResolver.openOutputStream(it) }
                stream?.let {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                    it.close()
                }

                runOnUiThread {
                    binding.progressLoading.visibility = View.GONE
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/jpeg"
                        putExtra(Intent.EXTRA_STREAM, uri)
                    }
                    startActivity(Intent.createChooser(shareIntent, "Share Image"))
                }

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    binding.progressLoading.visibility = View.GONE
                    Toast.makeText(this, "Share failed!", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}