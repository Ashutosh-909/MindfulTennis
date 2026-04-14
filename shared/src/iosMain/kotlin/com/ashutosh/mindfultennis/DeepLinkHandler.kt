package com.ashutosh.mindfultennis

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import platform.Foundation.NSURL

/**
 * Called from iOSApp.swift's onOpenURL to hand the OAuth callback URL
 * back to the Supabase SDK so it can exchange tokens and establish a session.
 * Handles both Apple and Google OAuth redirects via the PKCE flow.
 *
 * Usage in Swift:
 *   .onOpenURL { url in
 *       DeepLinkHandler.shared.handle(url: url.absoluteString)
 *   }
 */
object DeepLinkHandler : KoinComponent {

    private val supabaseClient: SupabaseClient by inject()

    fun handle(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        // handleDeeplinks checks scheme/host, dispatches internally — no coroutine needed
        supabaseClient.handleDeeplinks(nsUrl)
    }
}
