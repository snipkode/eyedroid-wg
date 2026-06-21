package com.wireguard.android.util

import com.wireguard.android.Application
import kotlinx.coroutines.CoroutineScope

val Any.applicationScope: CoroutineScope
    get() = Application.getCoroutineScope()
