package com.example.core.network

import com.example.BuildConfig

object SupabaseConfig {
    val supabaseUrl: String get() = BuildConfig.SUPABASE_URL.trim().removeSuffix("/")
    val supabaseAnonKey: String get() = BuildConfig.SUPABASE_ANON_KEY.trim()
}
