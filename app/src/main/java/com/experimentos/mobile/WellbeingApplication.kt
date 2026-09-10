package com.experimentos.mobile

import android.app.Application
import com.experimentos.mobile.shared.data.AppContainer

/** Provides application-wide dependencies without exposing credentials to the UI layer. */
class WellbeingApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }
}
