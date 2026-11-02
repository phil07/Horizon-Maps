package com.headunit.launcher

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The logbook (Settings > Fahrtenbuch) as a file to send: a CSV table for a spreadsheet, or a printable PDF with a page per month.
 * Each trip can be marked business or private (tapping it in Settings); unmarked trips count as private.
 */
object TripExport {
    /** The trips marked business, by their start time. */
    fun business(prefs: Prefs): Set<Long> = prefs.s(PreferenceKeys.TRIP_BUSINESS, "").split(',').mapNotNull { it.toLongOrNull() }.toSet()

    fun toggleBusiness(prefs: Prefs, start: Long) {
        val b = business(prefs)
        prefs.put(PreferenceKeys.TRIP_BUSINESS, (if (start in b) b - start else b + start).joinToString(","))
    }

    private fun day(t: Long) = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(Date(t))
    private fun time(t: Long) = SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date(t))
    private fun km(m: Double) = String.format(Locale.GERMANY, "%.1f", m / 1000.0)
    private fun dur(ms: Long): String { val m = (ms / 60000L).toInt(); return String.format(Locale.GERMANY, "%d:%02d", m / 60, m % 60) }

    /** The trips as CSV (semicolons and comma decimals, as German spreadsheets expect), oldest first. */
    internal fun csv(trips: List<Trip>, business: Set<Long>): String {
        fun cell(s: String) = if (s.contains(';') || s.contains('"') || s.contains('\n')) "\"" + s.replace("\"", "\"\"") + "\"" else s
        val sb = StringBuilder("Datum;Abfahrt;Ankunft;Ziel;Kilometer;Dauer;Fahrzeit;Höchstgeschwindigkeit (km/h);Art\r\n")
        for (t in trips.sortedBy { it.start }) {
            sb.append(listOf(day(t.start), time(t.start), time(t.end), cell(t.to), km(t.dist), dur(t.end - t.start), dur(t.moving * 1000L),
                Math.round(t.maxSpeed * 3.6).toString(), if (t.start in business) "Dienstlich" else "Privat").joinToString(";")).append("\r\n")
        }
        return sb.toString()
    }

    /** A PDF: a page per month (more when a month has many trips), with each month's business and private kilometres. */
    private fun pdf(trips: List<Trip>, business: Set<Long>, out: File) {
        val doc = PdfDocument()
        val w = 595; val h = 842 // A4 at 72 dpi
        val title = Paint().apply { textSize = 16f; isFakeBoldText = true; isAntiAlias = true }
        val head = Paint().apply { textSize = 9f; isFakeBoldText = true; isAntiAlias = true }
        val body = Paint().apply { textSize = 9f; isAntiAlias = true }
        val line = Paint().apply { strokeWidth = 0.5f; color = 0xFF999999.toInt() }
        val cols = floatArrayOf(36f, 96f, 160f, 340f, 400f, 450f, 520f)
        val heads = listOf("Datum", "Zeit", "Ziel", "km", "Dauer", "Max km/h", "Art")
        val months = trips.sortedBy { it.start }.groupBy { SimpleDateFormat("MMMM yyyy", Locale.GERMANY).format(Date(it.start)) }
        var pageNo = 0
        for ((month, list) in months) {
            val biz = list.filter { it.start in business }.sumOf { it.dist }
            val priv = list.filter { it.start !in business }.sumOf { it.dist }
            for (chunk in list.chunked(60)) {
                val page = doc.startPage(PdfDocument.PageInfo.Builder(w, h, ++pageNo).create())
                val c = page.canvas
                var y = 50f
                c.drawText("Fahrtenbuch · $month", 36f, y, title); y += 18f
                c.drawText("${list.size} Fahrten · ${km(biz + priv)} km · dienstlich ${km(biz)} km · privat ${km(priv)} km", 36f, y, body); y += 22f
                heads.forEachIndexed { i, s -> c.drawText(s, cols[i], y, head) }
                y += 4f; c.drawLine(36f, y, (w - 36).toFloat(), y, line); y += 12f
                for (t in chunk) {
                    val to = if (t.to.length > 32) t.to.take(31) + "…" else t.to
                    val cells = listOf(day(t.start), time(t.start) + "–" + time(t.end), to, km(t.dist), dur(t.end - t.start), Math.round(t.maxSpeed * 3.6).toString(),
                        if (t.start in business) "Dienstl." else "Privat")
                    cells.forEachIndexed { i, s -> c.drawText(s, cols[i], y, body) }
                    y += 12f
                }
                c.drawText("Horizon Nav · Seite $pageNo", 36f, (h - 30).toFloat(), body)
                doc.finishPage(page)
            }
        }
        if (pageNo == 0) {
            val page = doc.startPage(PdfDocument.PageInfo.Builder(w, h, 1).create())
            page.canvas.drawText("Fahrtenbuch: noch keine Fahrten", 36f, 50f, title)
            doc.finishPage(page)
        }
        out.outputStream().use { doc.writeTo(it) }
        doc.close()
    }

    /** Writes the logbook as CSV or PDF and opens the share sheet for it. Returns an error text, or null when it worked. */
    fun share(ctx: Context, prefs: Prefs, trips: List<Trip>, asPdf: Boolean): String? = try {
        val dir = File(ctx.getExternalFilesDir(null) ?: ctx.filesDir, "logbook").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val f = File(dir, "Fahrtenbuch-$stamp." + if (asPdf) "pdf" else "csv")
        val biz = business(prefs)
        if (asPdf) pdf(trips, biz, f) else f.writeText("﻿" + csv(trips, biz), Charsets.UTF_8)
        val uri = androidx.core.content.FileProvider.getUriForFile(ctx, "${ctx.packageName}.updates", f)
        val send = Intent(Intent.ACTION_SEND).setType(if (asPdf) "application/pdf" else "text/csv")
            .putExtra(Intent.EXTRA_STREAM, uri).putExtra(Intent.EXTRA_SUBJECT, "Fahrtenbuch $stamp")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ctx.startActivity(Intent.createChooser(send, "Fahrtenbuch teilen").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        null
    } catch (e: Exception) { e.message ?: e.javaClass.simpleName }
}
