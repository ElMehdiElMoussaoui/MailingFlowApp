package com.example.appmailing.fct_send

import android.content.Context
import android.net.Uri
import android.util.Base64
import java.io.InputStream

object SendGridUtils {
    /**
     * Converts an image from a URI to a Base64 String.
     * Use Base64.NO_WRAP for SendGrid compatibility.
     */
    fun encodeImageToBase64(context: Context, imageUri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            
            if (bytes != null) {
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Helper to get MIME type of a URI.
     */
    fun getMimeType(context: Context, uri: Uri): String {
        return context.contentResolver.getType(uri) ?: "image/png"
    }
}
