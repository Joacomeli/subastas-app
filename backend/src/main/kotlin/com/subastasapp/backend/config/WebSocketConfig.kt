package com.subastasapp.backend.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

// Nota: la app Android recibe las pujas por polling REST; este broker STOMP queda disponible
// para migrar a tiempo real por WebSocket.
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    @Value("\${app.ws.allowed-origins}") private val allowedOrigins: Array<String>
) : WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        config.enableSimpleBroker("/topic")
        config.setApplicationDestinationPrefixes("/app")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws-auction").setAllowedOriginPatterns(*allowedOrigins).withSockJS()
    }
}
