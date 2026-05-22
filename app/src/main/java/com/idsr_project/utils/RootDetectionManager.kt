package com.idsr_project.utils

import android.content.Context
import com.scottyab.rootbeer.RootBeer

object RootDetectionManager {

    fun isDeviceRooted(context: Context): Boolean{
        val rootBeer = RootBeer(context)
        return rootBeer.isRooted
    }
}