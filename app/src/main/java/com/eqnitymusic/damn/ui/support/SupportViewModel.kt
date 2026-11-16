package com.eqnitymusic.damn.ui.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eqnitymusic.damn.data.network.ApiService
import com.eqnitymusic.damn.data.network.EqnityInfoResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SupportViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _info = MutableStateFlow<EqnityInfoResponse?>(null)
    val info: StateFlow<EqnityInfoResponse?> = _info

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadInfo()
    }

    fun loadInfo() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = apiService.getEqnityInfo("https://eqnity.qzz.io/").execute()
                if (response.isSuccessful) {
                    _info.value = response.body()
                } else {
                    // Fallback to static data
                    _info.value = getFallbackInfo()
                }
            } catch (e: Exception) {
                _error.value = e.message
                _info.value = getFallbackInfo()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun getFallbackInfo(): EqnityInfoResponse {
        return EqnityInfoResponse(
            name = "Eqnity",
            tagline = "Developer · Creator · Minecraft nerd",
            about = "I make things that live on the web, in Discord, and inside Minecraft. If an idea is fun and codeable, I'll probably take a crack at it: websites, Discord bots, Minecraft clients, mods, and whatever else seems interesting at 2 AM.",
            skills = listOf(
                EqnityInfoResponse.Skill(
                    title = "Websites",
                    description = "Clean, animated, responsive sites — landing pages, portfolios, dashboards, storefronts. Built to look good and load fast.",
                    technologies = "HTML · CSS · JavaScript"
                ),
                EqnityInfoResponse.Skill(
                    title = "Discord Bots",
                    description = "Bots with commands, moderation, economy, games, logs and API integrations. From small utilities to full bot setups.",
                    technologies = "Commands · Economy · Games · API"
                ),
                EqnityInfoResponse.Skill(
                    title = "Minecraft Clients",
                    description = "Custom clients and PvP utilities for modern Minecraft — built on Fabric and tuned for smooth, responsive gameplay.",
                    technologies = "Fabric · PvP · Utilities"
                ),
                EqnityInfoResponse.Skill(
                    title = "Minecraft Mods",
                    description = "Gameplay and utility mods with custom features and clean GUIs. Small quality-of-life tweaks or full-featured mods.",
                    technologies = "Gameplay · Utility · GUIs"
                )
            ),
            contact = EqnityInfoResponse.ContactInfo(
                github = "@ItzEqnity",
                discord = "@eqnity"
            ),
            links = EqnityInfoResponse.Links(
                github = "https://github.com/ItzEqnity/ItzEqnity",
                discord = "https://discord.gg/PeWyrBzRgp"
            )
        )
    }
}