package com.makd.afinity.shared

import platform.UIKit.UIDevice

actual fun platformName(): String =
    UIDevice.currentDevice.systemName + " " + UIDevice.currentDevice.systemVersion
