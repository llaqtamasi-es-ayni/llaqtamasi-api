package pe.llaqtamasi.messaging;
import pe.llaqtamasi.service.SolicitudService;
import pe.llaqtamasi.repository.Db;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;

@Component
public class ChatEvents {
    private final SimpMessagingTemplate messages;
    private final Db db;
    private final int days;
    public ChatEvents(SimpMessagingTemplate messages,Db db,@Value("${app.chat-retencion-dias}") int days) {
        this.messages=messages; this.db=db; this.days=days;
    }
    @TransactionalEventListener
    public void sent(SolicitudService.MessageEvent event) {
        messages.convertAndSendToUser(event.client(),"/queue/mensajes",event.message());
        messages.convertAndSendToUser(event.provider(),"/queue/mensajes",event.message());
    }
    @Scheduled(fixedDelay=3600000)
    public void cleanup() {
        db.update("""
            DELETE m FROM mensaje m JOIN solicitud s ON s.id=m.solicitud_id
            WHERE s.estado='FINALIZADA' AND s.finalizada_en<TIMESTAMPADD(DAY,?,UTC_TIMESTAMP(6))
            """,-days);
    }
}
