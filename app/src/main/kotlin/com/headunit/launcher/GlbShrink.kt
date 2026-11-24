package com.headunit.launcher

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Makes a big binary glTF (.glb) small enough to use as your own arrow: models made with AI tools (Meshy and the like) are often
 * well over the limit because of their pictures (textures). Each picture embedded in the file is made smaller, step by step,
 * until the file fits; the shape itself isn't touched.
 */
internal object GlbShrink {
    private const val MAGIC = 0x46546C67      // "glTF"
    private const val JSON_CHUNK = 0x4E4F534A // "JSON"
    private const val BIN_CHUNK = 0x004E4942  // "BIN\0"

    /** Writes [src] into [dst] with its pictures made smaller until it's at most [limit] bytes. False when it can't be done. */
    fun shrink(src: File, dst: File, limit: Long): Boolean {
        val bytes = src.readBytes()
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        if (bytes.size < 28 || b.getInt(0) != MAGIC) return false
        val jsonLen = b.getInt(12)
        if (b.getInt(16) != JSON_CHUNK || 20 + jsonLen + 8 > bytes.size) return false
        val binAt = 20 + jsonLen
        val binLen = b.getInt(binAt)
        if (b.getInt(binAt + 4) != BIN_CHUNK || binAt + 8 + binLen > bytes.size) return false
        val bin = bytes.copyOfRange(binAt + 8, binAt + 8 + binLen)
        val json = String(bytes, 20, jsonLen, Charsets.UTF_8)
        for (maxPx in intArrayOf(2048, 1024, 512, 256)) {
            val out = rebuild(json, bin, maxPx) ?: return false
            if (out.size <= limit) { dst.writeBytes(out); return true }
        }
        return false
    }

    /** The whole file again, with every embedded picture at most [maxPx] on its longest side. */
    private fun rebuild(json: String, bin: ByteArray, maxPx: Int): ByteArray? {
        val j = JSONObject(json)
        val views = j.optJSONArray("bufferViews") ?: return null
        val images = j.optJSONArray("images")
        // which buffer views hold a picture, and of which image
        val imageOf = HashMap<Int, Int>()
        if (images != null) for (i in 0 until images.length()) {
            val v = images.getJSONObject(i).optInt("bufferView", -1)
            if (v >= 0) imageOf[v] = i
        }
        val out = ByteArrayOutputStream(bin.size)
        for (vi in 0 until views.length()) {
            val v = views.getJSONObject(vi)
            if (v.optInt("buffer", 0) != 0) return null
            val off = v.optInt("byteOffset", 0); val len = v.getInt("byteLength")
            if (off < 0 || off + len > bin.size) return null
            var data = bin.copyOfRange(off, off + len)
            imageOf[vi]?.let { ii ->
                val img = images!!.getJSONObject(ii)
                smaller(data, maxPx)?.let { (d, mime) -> data = d; img.put("mimeType", mime) }
            }
            // each view starts on a 4-byte boundary, as glTF asks
            while (out.size() % 4 != 0) out.write(0)
            v.put("byteOffset", out.size()); v.put("byteLength", data.size)
            out.write(data)
        }
        while (out.size() % 4 != 0) out.write(0)
        val newBin = out.toByteArray()
        j.getJSONArray("buffers").getJSONObject(0).put("byteLength", newBin.size)
        var jsonBytes = j.toString().toByteArray(Charsets.UTF_8)
        if (jsonBytes.size % 4 != 0) jsonBytes += ByteArray(4 - jsonBytes.size % 4) { ' '.code.toByte() }
        val total = 12 + 8 + jsonBytes.size + 8 + newBin.size
        val o = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN)
        o.putInt(MAGIC).putInt(2).putInt(total)
        o.putInt(jsonBytes.size).putInt(JSON_CHUNK).put(jsonBytes)
        o.putInt(newBin.size).putInt(BIN_CHUNK).put(newBin)
        return o.array()
    }

    /** The picture at most [maxPx] on its longest side (JPEG, or PNG where it has see-through parts); null to leave it as it is. */
    private fun smaller(data: ByteArray, maxPx: Int): Pair<ByteArray, String>? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
        val w = bounds.outWidth; val h = bounds.outHeight
        if (w <= 0 || h <= 0) return null   // a kind of picture Android can't read (KTX2, say): kept as it is
        if (maxOf(w, h) <= maxPx) return null
        var sample = 1
        while (maxOf(w, h) / (sample * 2) >= maxPx) sample *= 2
        val bmp = BitmapFactory.decodeByteArray(data, 0, data.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        val k = maxPx.toFloat() / maxOf(bmp.width, bmp.height)
        val scaled = if (k < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * k).toInt().coerceAtLeast(1), (bmp.height * k).toInt().coerceAtLeast(1), true) else bmp
        val alpha = scaled.hasAlpha()
        val o = ByteArrayOutputStream()
        scaled.compress(if (alpha) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 85, o)
        if (scaled !== bmp) scaled.recycle()
        bmp.recycle()
        return o.toByteArray() to (if (alpha) "image/png" else "image/jpeg")
    }
}
