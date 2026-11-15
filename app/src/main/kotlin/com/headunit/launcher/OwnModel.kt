package com.headunit.launcher

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Your own 3D model for the arrow: a binary glTF (.glb) of at most 8 MB, copied into the app. Its size is taken from its own
 * bounds, so it comes out as big as the built-in car whatever scale it was made at. The map loads it from a tiny server on the
 * phone itself (127.0.0.1), the same way the offline map is served.
 */
object OwnModel {
    const val MAX_BYTES = 8L * 1024 * 1024
    /** The built-in car is modelled in metres, 4.4 m long (see MapBinding.modelPxPerUnit). */
    const val CAR_LENGTH_M = 4.4
    private const val FILE = "own_model.glb"

    private fun file(ctx: Context) = File(ctx.filesDir, FILE)

    /** Copies the picked file in when it is a binary glTF of at most [MAX_BYTES]. The old model stays when it isn't. */
    fun save(ctx: Context, uri: Uri): Boolean {
        val tmp = File(ctx.filesDir, "$FILE.tmp")
        try {
            val input = ctx.contentResolver.openInputStream(uri) ?: return false
            var total = 0L
            input.use { inp -> tmp.outputStream().use { out ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val n = inp.read(buf)
                    if (n < 0) break
                    total += n
                    if (total > MAX_BYTES) return false
                    out.write(buf, 0, n)
                }
            } }
            val head = ByteArray(4)
            tmp.inputStream().use { it.read(head) }
            if (String(head, Charsets.US_ASCII) != "glTF") return false
            if (extent(tmp) == null) return false
            val dest = file(ctx)
            if (dest.exists()) dest.delete()
            return tmp.renameTo(dest)
        } catch (e: Exception) {
            return false
        } finally {
            if (tmp.exists()) tmp.delete()
        }
    }

    /** Removes the model, so the built-in car is shown again. */
    fun remove(ctx: Context) { file(ctx).delete() }

    /** The model's bounds, longest side in its own units. Null when the file can't be read as a binary glTF. */
    fun extent(f: File): Double? = try {
        val b = ByteBuffer.wrap(f.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
        if (b.limit() < 20 || b.getInt(0) != 0x46546C67) null else {
            val jsonLen = b.getInt(12)
            if (b.getInt(16) != 0x4E4F534A || jsonLen <= 0 || 20 + jsonLen > b.limit()) null else {
                val j = JSONObject(String(b.array(), 20, jsonLen, Charsets.UTF_8))
                val acc = j.optJSONArray("accessors")
                val lo = doubleArrayOf(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE)
                val hi = doubleArrayOf(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE)
                var found = false
                val meshes = j.optJSONArray("meshes")
                if (acc != null && meshes != null) for (mi in 0 until meshes.length()) {
                    val prims = meshes.getJSONObject(mi).optJSONArray("primitives") ?: continue
                    for (pi in 0 until prims.length()) {
                        val pos = prims.getJSONObject(pi).optJSONObject("attributes")?.optInt("POSITION", -1) ?: -1
                        val a = acc.optJSONObject(pos) ?: continue
                        val mn = a.optJSONArray("min") ?: continue
                        val mx = a.optJSONArray("max") ?: continue
                        if (mn.length() < 3 || mx.length() < 3) continue
                        found = true
                        for (k in 0 until 3) { lo[k] = minOf(lo[k], mn.getDouble(k)); hi[k] = maxOf(hi[k], mx.getDouble(k)) }
                    }
                }
                if (!found) null else (0 until 3).maxOf { hi[it] - lo[it] }.takeIf { it.isFinite() && it > 0.0 }
            }
        }
    } catch (e: Exception) { null }

    /** Size factor for the arrow: the built-in car's length over this model's longest side. Null when there's no usable model. */
    @Volatile private var cachedStamp = -1L
    @Volatile private var cachedFactor: Double? = null
    fun factor(ctx: Context): Double? {
        val f = file(ctx)
        if (!f.isFile) return null
        val stamp = f.lastModified()
        if (stamp != cachedStamp) {
            cachedStamp = stamp
            cachedFactor = extent(f)?.let { CAR_LENGTH_M / it }
        }
        return cachedFactor
    }

    /** Changes whenever the model file is replaced, so the map loads it again (the server adds it to the address). */
    fun stamp(ctx: Context): Long = file(ctx).takeIf { it.isFile }?.lastModified() ?: 0L

    private var port = 0
    @Volatile private var server: ServerSocket? = null

    /** The address the map loads the model from (starts the local server when it isn't running). */
    @Synchronized
    fun url(ctx: Context): String? {
        if (!file(ctx).isFile) return null
        if (server == null || server!!.isClosed) {
            val s = ServerSocket(0, 8, InetAddress.getByName("127.0.0.1"))
            server = s; port = s.localPort
            val app = ctx.applicationContext
            Thread({
                while (!s.isClosed) {
                    val c = try { s.accept() } catch (e: Exception) { break }
                    try { serve(c, file(app)) } catch (e: Exception) { try { c.close() } catch (x: Exception) { } }
                }
            }, "own-model").apply { isDaemon = true }.start()
        }
        return "http://127.0.0.1:$port/$FILE?v=${stamp(ctx)}"
    }

    private fun serve(c: java.net.Socket, f: File) {
        c.use { sock ->
            sock.soTimeout = 10_000
            val inp = sock.getInputStream().bufferedReader(Charsets.ISO_8859_1)
            // (the request's headers are read and ignored: the one file is all there is to answer)
            while (true) { val l = inp.readLine() ?: break; if (l.isEmpty()) break }
            val out = sock.getOutputStream()
            if (!f.isFile || f.length() > MAX_BYTES) {
                out.write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
            } else {
                val body = f.readBytes()
                out.write(("HTTP/1.1 200 OK\r\nContent-Type: model/gltf-binary\r\nContent-Length: ${body.size}\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n").toByteArray())
                out.write(body)
            }
            out.flush()
        }
    }
}
