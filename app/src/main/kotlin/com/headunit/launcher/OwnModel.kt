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
    /** The biggest file that can be picked: over [MAX_BYTES] its pictures are made smaller first (see GlbShrink). */
    const val MAX_PICK = 48L * 1024 * 1024
    /** The built-in car is modelled in metres, 4.4 m long (see MapBinding.modelPxPerUnit). */
    const val CAR_LENGTH_M = 4.4
    private const val FILE = "own_model.glb"

    private fun file(ctx: Context) = File(ctx.filesDir, FILE)

    /** Copies the picked file in when it is a binary glTF of at most [MAX_BYTES] (a bigger one, up to [MAX_PICK], gets its pictures
     *  made smaller first). The old model stays when it isn't. */
    fun save(ctx: Context, uri: Uri): Boolean {
        val tmp = File(ctx.filesDir, "$FILE.tmp")
        val small = File(ctx.filesDir, "$FILE.small")
        try {
            val input = ctx.contentResolver.openInputStream(uri) ?: return false
            var total = 0L
            input.use { inp -> tmp.outputStream().use { out ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val n = inp.read(buf)
                    if (n < 0) break
                    total += n
                    if (total > MAX_PICK) return false
                    out.write(buf, 0, n)
                }
            } }
            val head = ByteArray(4)
            tmp.inputStream().use { it.read(head) }
            if (String(head, Charsets.US_ASCII) != "glTF") return false
            // too big: its pictures are made smaller (the shape isn't changed) until it fits
            val use = if (total > MAX_BYTES) { if (!GlbShrink.shrink(tmp, small, MAX_BYTES)) return false; small } else tmp
            if (extent(use) == null) return false
            val dest = file(ctx)
            if (dest.exists()) dest.delete()
            return use.renameTo(dest)
        } catch (e: Throwable) {
            return false
        } finally {
            if (tmp.exists()) tmp.delete()
            if (small.exists()) small.delete()
        }
    }

    /** Removes the model, so the built-in car is shown again. */
    fun remove(ctx: Context) { file(ctx).delete() }

    /** The model's size: its longest side, measured the way the map draws it. Each mesh's bounds are moved by the transforms of its
     *  nodes (a model that scales itself down in its scene comes out at its real size). Null when the file can't be read. */
    fun extent(f: File): Double? = try {
        val b = ByteBuffer.wrap(f.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
        if (b.limit() < 20 || b.getInt(0) != 0x46546C67) null else {
            val jsonLen = b.getInt(12)
            if (b.getInt(16) != 0x4E4F534A || jsonLen <= 0 || 20 + jsonLen > b.limit()) null else {
                val j = JSONObject(String(b.array(), 20, jsonLen, Charsets.UTF_8))
                val acc = j.optJSONArray("accessors")
                val meshes = j.optJSONArray("meshes")
                val nodes = j.optJSONArray("nodes")
                if (acc == null || meshes == null || nodes == null) null else {
                    val lo = DoubleArray(3) { Double.MAX_VALUE }
                    val hi = DoubleArray(3) { -Double.MAX_VALUE }
                    var found = false
                    fun walk(idx: Int, parent: DoubleArray) {
                        val n = nodes.optJSONObject(idx) ?: return
                        val world = mul(parent, localMatrix(n))
                        val mi = n.optInt("mesh", -1)
                        val prims = if (mi >= 0) meshes.optJSONObject(mi)?.optJSONArray("primitives") else null
                        if (prims != null) for (pi in 0 until prims.length()) {
                            val pos = prims.getJSONObject(pi).optJSONObject("attributes")?.optInt("POSITION", -1) ?: -1
                            val a = acc.optJSONObject(pos) ?: continue
                            val mn = a.optJSONArray("min") ?: continue
                            val mx = a.optJSONArray("max") ?: continue
                            if (mn.length() < 3 || mx.length() < 3) continue
                            found = true
                            // the eight corners of the mesh's box, moved by the node's transform, give its bounds in the scene
                            for (cx in 0..1) for (cy in 0..1) for (cz in 0..1) {
                                val q = doubleArrayOf(if (cx == 0) mn.getDouble(0) else mx.getDouble(0), if (cy == 0) mn.getDouble(1) else mx.getDouble(1), if (cz == 0) mn.getDouble(2) else mx.getDouble(2))
                                val w = apply(world, q)
                                for (k in 0 until 3) { lo[k] = minOf(lo[k], w[k]); hi[k] = maxOf(hi[k], w[k]) }
                            }
                        }
                        val kids = n.optJSONArray("children")
                        if (kids != null) for (i in 0 until kids.length()) walk(kids.getInt(i), world)
                    }
                    // the scene's own top nodes; without a scene, the nodes that no other node holds
                    val isChild = HashSet<Int>()
                    for (i in 0 until nodes.length()) nodes.optJSONObject(i)?.optJSONArray("children")?.let { c -> for (k in 0 until c.length()) isChild.add(c.getInt(k)) }
                    val scene = j.optJSONArray("scenes")?.optJSONObject(j.optInt("scene", 0))?.optJSONArray("nodes")
                    val identity = DoubleArray(16) { if (it % 5 == 0) 1.0 else 0.0 }
                    if (scene != null) {
                        for (i in 0 until scene.length()) walk(scene.getInt(i), identity)
                    } else {
                        for (i in 0 until nodes.length()) if (i !in isChild) walk(i, identity)
                    }
                    if (!found) null else (0 until 3).maxOf { hi[it] - lo[it] }.takeIf { it.isFinite() && it > 0.0 }
                }
            }
        }
    } catch (e: Exception) { null }

    /** Row-major product a * b of two 4x4 matrices. */
    private fun mul(a: DoubleArray, b: DoubleArray): DoubleArray = DoubleArray(16) { i ->
        val r = i / 4; val c = i % 4
        var s = 0.0
        for (k in 0 until 4) s += a[r * 4 + k] * b[k * 4 + c]
        s
    }

    /** A point moved by a row-major 4x4 matrix. */
    private fun apply(m: DoubleArray, p: DoubleArray): DoubleArray = DoubleArray(3) { r ->
        m[r * 4] * p[0] + m[r * 4 + 1] * p[1] + m[r * 4 + 2] * p[2] + m[r * 4 + 3]
    }

    /** A node's own transform, row-major: its matrix (glTF stores it column by column), or translation, rotation and scale. */
    private fun localMatrix(n: JSONObject): DoubleArray {
        val m = n.optJSONArray("matrix")
        if (m != null && m.length() == 16) return DoubleArray(16) { i -> m.getDouble((i % 4) * 4 + i / 4) }
        val t = n.optJSONArray("translation")
        val q = n.optJSONArray("rotation")
        val s = n.optJSONArray("scale")
        val tx = t?.getDouble(0) ?: 0.0; val ty = t?.getDouble(1) ?: 0.0; val tz = t?.getDouble(2) ?: 0.0
        val qx = q?.getDouble(0) ?: 0.0; val qy = q?.getDouble(1) ?: 0.0; val qz = q?.getDouble(2) ?: 0.0; val qw = q?.getDouble(3) ?: 1.0
        val sx = s?.getDouble(0) ?: 1.0; val sy = s?.getDouble(1) ?: 1.0; val sz = s?.getDouble(2) ?: 1.0
        val r00 = 1 - 2 * (qy * qy + qz * qz); val r01 = 2 * (qx * qy - qz * qw); val r02 = 2 * (qx * qz + qy * qw)
        val r10 = 2 * (qx * qy + qz * qw); val r11 = 1 - 2 * (qx * qx + qz * qz); val r12 = 2 * (qy * qz - qx * qw)
        val r20 = 2 * (qx * qz - qy * qw); val r21 = 2 * (qy * qz + qx * qw); val r22 = 1 - 2 * (qx * qx + qy * qy)
        // translation * rotation * scale
        return doubleArrayOf(
            r00 * sx, r01 * sy, r02 * sz, tx,
            r10 * sx, r11 * sy, r12 * sz, ty,
            r20 * sx, r21 * sy, r22 * sz, tz,
            0.0, 0.0, 0.0, 1.0)
    }

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
                    try { serve(c, file(app)) } catch (e: Exception) { try { c.close() } catch (x: Exception) { softFail(x) } }
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
