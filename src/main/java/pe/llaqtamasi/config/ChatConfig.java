package pe.llaqtamasi.config;
import pe.llaqtamasi.security.Access;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.time.Instant;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.context.event.EventListener;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.config.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class ChatConfig implements WebSocketMessageBrokerConfigurer {
    private final JwtDecoder decoder;
    private final Access access;
    private final String[] origins;
    private final ConcurrentMap<String, Jwt> sessions = new ConcurrentHashMap<>();
    public ChatConfig(JwtDecoder decoder,Access access,@Value("${app.cors-origins}") String origins) {
        this.decoder=decoder; this.access=access; this.origins=origins.split(",");
    }
    @Override public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins(origins);
    }
    @Override public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue");
        registry.setUserDestinationPrefix("/user");
    }
    @Override public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override public Message<?> preSend(Message<?> message,MessageChannel channel) {
                var header=MessageHeaderAccessor.getAccessor(message,StompHeaderAccessor.class);
                if(header==null || header.getCommand()==null) return message;
                if(header.getCommand()==StompCommand.CONNECT) {
                    String auth=header.getFirstNativeHeader("Authorization");
                    if(auth==null || !auth.startsWith("Bearer ")) throw new IllegalArgumentException("Token requerido");
                    var jwt=decoder.decode(auth.substring(7));
                    access.user(jwt);
                    header.setUser(new JwtAuthenticationToken(jwt,List.of(),jwt.getSubject()));
                    sessions.put(header.getSessionId(), jwt);
                } else if(header.getCommand()==StompCommand.SUBSCRIBE) {
                    if(!(header.getUser() instanceof JwtAuthenticationToken token))
                        throw new IllegalArgumentException("Sesion requerida");
                    access.user(decoder.decode(token.getToken().getTokenValue()));
                    if(!"/user/queue/mensajes".equals(header.getDestination()))
                        throw new IllegalArgumentException("Destino no autorizado");
                } else if(header.getCommand()==StompCommand.SEND) {
                    throw new IllegalArgumentException("Envia mensajes mediante POST /api/solicitudes/{id}/mensajes");
                }
                return message;
            }
        });
    }
    @Override public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override public Message<?> preSend(Message<?> message, MessageChannel channel) {
                var header = SimpMessageHeaderAccessor.wrap(message);
                if (header.getMessageType() != SimpMessageType.MESSAGE) return message;
                Jwt jwt = sessions.get(header.getSessionId());
                if (jwt == null || jwt.getExpiresAt() == null || !jwt.getExpiresAt().isAfter(Instant.now())) {
                    return null;
                }
                try {
                    access.user(jwt);
                    return message;
                } catch (org.springframework.web.server.ResponseStatusException inactive) {
                    return null;
                }
            }
        });
    }
    @EventListener
    public void disconnected(SessionDisconnectEvent event) {
        sessions.remove(event.getSessionId());
    }

}
