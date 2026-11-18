package se.example.photovisit

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface

data class Photo(val uri: Uri, val name: String, val takenAt: Long, val lat: Double?, val lon: Double?)

object Geotagger {
    /** Läser de senaste fotona direkt ur MediaStore. Med ACCESS_MEDIA_LOCATION får vi originalets platsdata. */
    fun scan(c: Context, limit: Int = 300): List<Photo> {
        val out = mutableListOf<Photo>()
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val proj = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN
        )
        c.contentResolver.query(collection, proj, null, null, "${MediaStore.Images.Media.DATE_TAKEN} DESC")?.use {
            while (it.moveToNext() && out.size < limit) {
                val uri = ContentUris.withAppendedId(collection, it.getLong(0))
                val ll = readLatLong(c, uri)
                out += Photo(uri, it.getString(1) ?: "", it.getLong(2), ll?.get(0), ll?.get(1))
            }
        }
        return out
    }

    private fun readLatLong(c: Context, uri: Uri): DoubleArray? = try {
        c.contentResolver.openInputStream(MediaStore.setRequireOriginal(uri))?.use {
            ExifInterface(it).latLong
        }
    } catch (e: Exception) { null }
}
