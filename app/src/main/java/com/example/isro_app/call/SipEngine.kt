package com.example.isro_app.call

import android.content.Context
import org.linphone.core.Factory
import org.linphone.core.Core

object SipEngine {

    private var core: Core? = null

    fun init(context: Context) {
        val factory = Factory.instance()
        factory.setDebugMode(true, "Linphone")

        core = factory.createCore(null, null, context)
        core?.start()
    }
}
