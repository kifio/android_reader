package me.kifio.kreader.android.utils.extensions

import org.readium.r2.shared.publication.Link

val Link.outlineTitle: String
    get() = title ?: href
