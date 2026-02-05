package com.idsr_project.activities

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.idsr_project.R
import com.idsr_project.databinding.ActivityImgPreviewBinding
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.OutputStream

class Img_Preview_Activity : AppCompatActivity() {
    private lateinit var binding: ActivityImgPreviewBinding

    private val client = OkHttpClient.Builder().build()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityImgPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUri  = intent.getStringExtra("imageUri")

        binding.fullImage.animate().alpha(1f).setDuration(300).start()

        Glide.with(this)
            .load(imageUri)
            .into(binding.fullImage)

        binding.btnClose.setOnClickListener { finish() }


        binding.btnDownload.setOnClickListener {
            if (imageUri != null) {
                downloadImage(imageUri)
            }
        }

        binding.btnShare.setOnClickListener {
            if (imageUri != null) {
                shareImage(imageUri)
            }
        }

    }
    private fun downloadImage(url: String) {
        Thread {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                val bytes = response.body?.bytes()
                if (bytes == null) {
                    runOnUiThread { Toast.makeText(this, "Download failed!", Toast.LENGTH_LONG).show() }
                    return@Thread
                }

                val filename = "image_${System.currentTimeMillis()}.jpg"
                val fos: OutputStream?

                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/IDSRSaved")
                }

                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = uri?.let { contentResolver.openOutputStream(it) }

                fos?.write(bytes)
                fos?.close()

                runOnUiThread {
                    Toast.makeText(this, "Image saved to gallery!", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Save failed!", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
    private fun shareImage(url: String) {
        Thread {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val bytes = response.body?.bytes() ?: return@Thread

                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                val fileName = "shared_image_${System.currentTimeMillis()}.jpg"
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                }

                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                val stream = uri?.let { contentResolver.openOutputStream(it) }

                stream?.let {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                    it.close()
                }

                runOnUiThread {
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.type = "image/jpeg"
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri)

                    startActivity(Intent.createChooser(shareIntent, "Share Image"))
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

}
