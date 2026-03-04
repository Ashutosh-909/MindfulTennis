package com.ashutosh.mindfultennis.util

import java.util.UUID

/**
 * General-purpose extension functions.
 */

/**
 * Generates a new random UUID string.
 */
fun generateId(): String = UUID.randomUUID().toString()
