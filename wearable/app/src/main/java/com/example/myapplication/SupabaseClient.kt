package com.example.myapplication

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.functions.Functions

// Bu değerleri backend arkadaşından aldığın gerçek değerlerle değiştir
private const val SUPABASE_URL = "https://upckhjcluoqdmymlirnt.supabase.co"
private const val SUPABASE_ANON_KEY = "sb_publishable_aWcnIX4IIVbkCFO0Okhr3w_u2iuYZMh"

val supabase = createSupabaseClient(
    supabaseUrl = SUPABASE_URL,
    supabaseKey = SUPABASE_ANON_KEY
) {
    install(Postgrest)
    install(Auth)
    install(Functions)
}