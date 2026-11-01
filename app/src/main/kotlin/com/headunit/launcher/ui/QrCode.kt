package com.headunit.launcher.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/** A QR code of [text], black on white, [px] pixels square (null if it couldn't be made). */
internal fun qrCode(text: String, px: Int): ImageBitmap? = try {
    val m = com.google.zxing.qrcode.QRCodeWriter().encode(text, com.google.zxing.BarcodeFormat.QR_CODE, px, px,
        mapOf(com.google.zxing.EncodeHintType.MARGIN to 1))
    val w = m.width; val h = m.height
    val pixels = IntArray(w * h)
    for (y in 0 until h) for (x in 0 until w) pixels[y * w + x] = if (m.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
    android.graphics.Bitmap.createBitmap(pixels, w, h, android.graphics.Bitmap.Config.ARGB_8888).asImageBitmap()
} catch (e: Throwable) { null }
