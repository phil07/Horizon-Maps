package com.headunit.launcher

import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * "Where you parked" when the phone leaves the car's Bluetooth (also without Android Auto): the car is recognised by its
 * Bluetooth class (car audio / hands-free kit) or because it was connected when Android Auto started. The spot is where the
 * phone last was, as long as that was recent and you'd stopped.
 */
object CarBluetooth {
    /** The phone's last position from the app's own GPS (kept in memory: a fresh one is needed, not a rounded or old one). */
    @Volatile private var last: Triple<LL, Float, Long>? = null

    fun noteFix(p: LL, speed: Float) { last = Triple(p, speed, System.currentTimeMillis()) }

    /** The car devices learned so far (their Bluetooth addresses). */
    private fun known(prefs: Prefs): Set<String> = prefs.s(PreferenceKeys.CAR_BT_DEVICES, "").split(',').filter { it.isNotBlank() }.toSet()

    /** Android Auto just started: a device that connected in the last 3 minutes is the car. */
    fun carStarted(ctx: Context) {
        val prefs = Prefs(ctx.applicationContext)
        val raw = prefs.s(PreferenceKeys.CAR_BT_LAST, "").split('|')
        val addr = raw.getOrNull(0) ?: return
        val at = raw.getOrNull(1)?.toLongOrNull() ?: return
        if (addr.isBlank() || System.currentTimeMillis() - at > 180_000L) return
        val k = known(prefs)
        if (addr !in k) prefs.put(PreferenceKeys.CAR_BT_DEVICES, (k + addr).take(8).joinToString(","))
    }

    internal fun isCarClass(major: Int, device: Int): Boolean =
        major == BluetoothClass.Device.Major.AUDIO_VIDEO &&
            (device == BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO || device == BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE)

    /** Whether a position from [atMs] (moving at [speed] m/s) is good for the parked spot at [nowMs]. */
    internal fun usable(speed: Float, atMs: Long, nowMs: Long): Boolean = speed < 2.5f && nowMs - atMs in 0..300_000L

    @Suppress("MissingPermission")
    fun onBluetooth(ctx: Context, intent: Intent) {
        val prefs = Prefs(ctx.applicationContext)
        if (!prefs.b(PreferenceKeys.PARK_BLUETOOTH, true)) return
        val dev: BluetoothDevice? = try {
            if (android.os.Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            else @Suppress("DEPRECATION") intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
        } catch (e: Exception) { null }
        val addr = try { dev?.address } catch (e: SecurityException) { null } ?: return
        val now = System.currentTimeMillis()
        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                prefs.put(PreferenceKeys.CAR_BT_LAST, "$addr|$now")
                // back in the car: the parked spot isn't needed any more
                if (addr in known(prefs)) prefs.put(PreferenceKeys.PARKED_SPOT, "")
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val cls = try { dev?.bluetoothClass } catch (e: SecurityException) { null }
                val car = addr in known(prefs) || (cls != null && isCarClass(cls.majorDeviceClass, cls.deviceClass))
                if (!car) return
                val fix = last?.takeIf { usable(it.second, it.third, now) }?.first ?: lastKnown(ctx, now) ?: return
                prefs.put(PreferenceKeys.PARKED_SPOT, String.format(java.util.Locale.US, "%.6f,%.6f,%d", fix.lat, fix.lon, now))
                android.util.Log.w("CarBluetooth", "parked spot saved (car Bluetooth disconnected)")
            }
        }
    }

    /** The system's last GPS position, when the app's own one isn't there (the app wasn't running): only a fresh one. */
    @Suppress("MissingPermission")
    private fun lastKnown(ctx: Context, now: Long): LL? = try {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        listOf(android.location.LocationManager.GPS_PROVIDER, android.location.LocationManager.NETWORK_PROVIDER)
            .mapNotNull { try { lm.getLastKnownLocation(it) } catch (e: Exception) { null } }
            .filter { usable(if (it.hasSpeed()) it.speed else 0f, it.time, now) && (!it.hasAccuracy() || it.accuracy < 80f) }
            .maxByOrNull { it.time }?.let { LL(it.latitude, it.longitude) }
    } catch (e: Exception) { null }
}

/** Tells [CarBluetooth] when a Bluetooth device connects to or leaves the phone. */
class CarBluetoothReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try { CarBluetooth.onBluetooth(context, intent) } catch (e: Exception) { }
    }
}
