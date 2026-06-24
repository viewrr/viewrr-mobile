package com.makd.afinity.shared.ui.library

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val libraryModule = module { viewModelOf(::LibraryViewModel) }
