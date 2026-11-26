package com.headunit.launcher

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * The app's plain web requests, in one place: the timeouts (each call names its own), the app's name for the servers, the status
 * check and closing the connection. A service that words its own error for a status passes [failure]; by default that is
 * "HTTP <code>".
 */
internal object Http {
    /** Said to every server, so they can tell the app apart. */
    const val AGENT = "HorizonNav/1.0 (Android navigation app)"

    fun get(
        url: String,
        connectMs: Int,
        readMs: Int,
        accept: String? = null,
        failure: (code: Int, body: String) -> Exception = { code, _ -> IOException("HTTP $code") },
    ): String = request(url, null, connectMs, readMs, accept, failure)

    fun post(
        url: String,
        body: String,
        connectMs: Int,
        readMs: Int,
        failure: (code: Int, body: String) -> Exception = { code, _ -> IOException("HTTP $code") },
    ): String = request(url, body, connectMs, readMs, null, failure)

    private fun request(
        url: String,
        body: String?,
        connectMs: Int,
        readMs: Int,
        accept: String?,
        failure: (code: Int, body: String) -> Exception,
    ): String {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = connectMs
            c.readTimeout = readMs
            c.setRequestProperty("User-Agent", AGENT)
            accept?.let { c.setRequestProperty("Accept", it) }
            if (body != null) {
                c.requestMethod = "POST"
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                c.outputStream.use { it.write(body.toByteArray()) }
            }
            val code = c.responseCode
            val text = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) throw failure(code, text)
            return text
        } finally {
            c.disconnect()
        }
    }
}
