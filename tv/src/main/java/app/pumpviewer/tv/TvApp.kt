package app.pumpviewer.tv

import android.app.Application
import app.pumpviewer.data.Repo

class TvApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Repo.init(this)
    }
}
