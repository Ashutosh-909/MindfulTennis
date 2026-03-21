package com.ashutosh.mindfultennis.util

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * General-purpose extension functions.
 */

/**
 * Generates a new random UUID string.
 */
@OptIn(ExperimentalUuidApi::class)
fun generateId(): String = Uuid.random().toString()
