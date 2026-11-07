package pe.llaqtamasi.service;
import pe.llaqtamasi.repository.Db;
import pe.llaqtamasi.security.Access;

import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static pe.llaqtamasi.dto.Requests.*;

class RulesTest {
    Db db;
    SolicitudService service;
    String user="cliente";
    @BeforeEach void setup() {
        db=mock(Db.class);
        doAnswer(invocation -> {
            if (!(boolean)invocation.getArgument(0)) throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
            return null;
        }).when(db).allow(anyBoolean());
        doAnswer(invocation -> {
            if (!(boolean)invocation.getArgument(0)) throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST);
            return null;
        }).when(db).require(anyBoolean(),anyString());
        service=new SolicitudService(db,mock(Access.class),mock(ApplicationEventPublisher.class));
    }
    void request(String state) {
        when(db.one(anyString(),eq("solicitud"))).thenReturn(Map.of("vecino_id","cliente","proveedor_id","proveedor","estado",state));
    }
    @Test void outsiderCannotReadChat() {
        request("ACEPTADA");
        assertThrows(ResponseStatusException.class,()->service.participant("intruso","solicitud",false));
    }
    @Test void clientCannotAcceptRequest() {
        request("PENDIENTE");
        assertThrows(ResponseStatusException.class,()->service.state(user,"solicitud",EstadoSolicitud.ACEPTADA));
    }
    @Test void pendingRequestCannotFinish() {
        request("PENDIENTE");
        assertThrows(ResponseStatusException.class,()->service.state(user,"solicitud",EstadoSolicitud.FINALIZADA));
    }
    @Test void closedChatCannotReceiveMessages() {
        request("FINALIZADA");
        assertThrows(ResponseStatusException.class,()->service.message(user,"solicitud",new Mensaje(UUID.randomUUID(),"hola")));
    }
    @Test void providerCanAcceptPendingRequest() {
        request("PENDIENTE");
        service.state("proveedor","solicitud",EstadoSolicitud.ACEPTADA);
        verify(db).update("UPDATE solicitud SET estado=? WHERE id=?","ACEPTADA","solicitud");
    }
    @Test void invalidVoteRejectedByValidation() {
        try(var factory=jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            assertFalse(factory.getValidator().validate(new Voto(6)).isEmpty());
            assertTrue(factory.getValidator().validate(new Voto(5)).isEmpty());
        }
    }
    @Test void duplicateSyncDoesNotRepeatMutation() {
        UUID id=UUID.randomUUID();
        when(db.list("SELECT usuario_id FROM accion_sync WHERE id_accion=?",id.toString()))
            .thenReturn(List.of(Map.of("usuario_id","cliente")));
        var community=mock(CommunityService.class);
        var requests=mock(SolicitudService.class);
        var sync=new SyncService(db,community,requests);
        var action=new Accion(id,Operacion.AVAL,UUID.randomUUID(),null,null,null,null,null,null,null,null,null);
        assertEquals("YA_APLICADA",sync.apply("cliente",action).get("estado"));
        verifyNoInteractions(community,requests);
    }
    @Test void otherAccountCannotReuseSyncAction() {
        UUID id=UUID.randomUUID();
        when(db.list("SELECT usuario_id FROM accion_sync WHERE id_accion=?",id.toString()))
            .thenReturn(List.of(Map.of("usuario_id","otra")));
        var sync=new SyncService(db,mock(CommunityService.class),mock(SolicitudService.class));
        var action=new Accion(id,Operacion.AVAL,UUID.randomUUID(),null,null,null,null,null,null,null,null,null);
        assertThrows(ResponseStatusException.class,()->sync.apply("cliente",action));
    }
}
