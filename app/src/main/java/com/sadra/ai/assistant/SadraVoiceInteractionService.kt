package com.sadra.ai.assistant

import android.service.voice.VoiceInteractionService

/** Entry point Android binds to when SADRA AI holds ROLE_ASSISTANT. Intentionally empty: no background work while idle. */
class SadraVoiceInteractionService : VoiceInteractionService()
