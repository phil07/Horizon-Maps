package com.headunit.launcher.car

import android.app.Activity
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView

/**
 * Only here so Android Auto lists Horizon Nav in its app launcher (CAR_LAUNCHER).
 * Apps without such an activity show up only under the navigation apps, which
 * is why sideloaded builds looked missing. The navigation itself runs in CarNavService.
 */
class CarLaunchActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            text = "Horizon Nav – die Navigation läuft über die Karte von Android Auto."
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        })
    }
}
