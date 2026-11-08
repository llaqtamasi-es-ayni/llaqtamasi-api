# Backend de Llaqtamasi es Ayni

API de Java 21, Spring Boot 4.0.9 y MySQL 8.4. Incluye el esquema aprobado de 19 tablas, acceso JWT, perfiles, confianza, solicitudes, mensajeria, muro, reportes, centros, roles y sincronizacion.

## Arranque en Windows

1. Abre MySQL Workbench y ejecuta `database/crear.sql` en una base nueva. Si ya tienes las 19 tablas, usa esa base directamente. Si tienes la version inicial de 17 tablas, ejecuta una sola vez `database/actualizar-17-tablas.sql`.
2. Abre PowerShell en esta carpeta.
3. Configura las variables siguientes con tus credenciales de MySQL y una cuenta administradora nueva.
4. Ejecuta el archivo JAR.

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/llaqtamasi_ayni?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true'
$env:DB_USER='tu_usuario_mysql'
$env:DB_PASSWORD='tu_clave_mysql'
$env:JWT_SECRET='reemplaza-por-un-secreto-aleatorio-de-al-menos-32-bytes'
$env:ADMIN_LOGIN='administrador'
$env:ADMIN_PASSWORD='reemplaza-por-una-clave-de-12-o-mas-caracteres'
$env:CORS_ORIGINS='http://localhost:4200,http://localhost:8100'
java -jar .\target\backend-1.0.0.jar
```

La API queda en `http://localhost:8080/api` y el WebSocket en `ws://localhost:8080/ws`. Comprueba el arranque con `GET /api/salud`.

Conserva el mismo JWT_SECRET entre reinicios para mantener validos los tokens emitidos. Se puede generar un secreto aleatorio con PowerShell 7.

```powershell
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
```

ADMIN_LOGIN y ADMIN_PASSWORD crean una cuenta nueva, activa, verificada y con rol ADMIN. Si el nombre de acceso ya existe, el arranque conserva esa cuenta y sus permisos. Una vez creada, puedes quitar ambas variables para los siguientes arranques.

Los datos de prueba usados durante la verificacion se guardaron en una instancia temporal separada. Debes configurar tu propia conexion para utilizar la app.

## Compilacion y pruebas

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.12.1'
.\mvnw.cmd "-Dmaven.repo.local=$PWD\.tools\repository" clean package
```

El lanzador usa Maven 3.9.9 de `.tools`. Si copias el proyecto sin esa carpeta, ejecuta `.\preparar.ps1` para descargar Maven.

El resultado es `target/backend-1.0.0.jar`. Se incluyeron ocho pruebas unitarias sobre permisos, estados, calificaciones y acciones repetidas.

La prueba funcional `tests/smoke.py` requiere Python 3 y una API en marcha con una cuenta ADMIN. Genera cuentas y datos nuevos, por lo que debe ejecutarse en una base de prueba.

```powershell
$env:API_URL='http://localhost:8080'
$env:ADMIN_LOGIN='administrador'
$env:ADMIN_PASSWORD='tu-clave-administradora'
python .\tests\smoke.py
```

Se comprobo el flujo real sobre MySQL 8.4, la cobertura geografica, las restricciones de permisos, el chat REST y STOMP, los votos, avales, reportes, roles, paquetes y la sincronizacion concurrente. Tambien se comprobo el rollback de una accion fallida y su posterior reintento.

## Autenticacion

`POST /api/auth/registro`

```json
{
  "alias": "Rosa",
  "nombreAcceso": "rosa.vecina",
  "password": "Una-clave-larga-y-personal"
}
```

`POST /api/auth/login`

```json
{
  "nombreAcceso": "rosa.vecina",
  "password": "Una-clave-larga-y-personal"
}
```

La respuesta contiene accessToken, tokenType, expiresIn y usuarioId. Los tokens duran una hora. Envia `Authorization: Bearer TOKEN` en las otras rutas. El usuario puede volver a iniciar sesion cuando expire su token. Las contrasenas se guardan con BCrypt. Los UUID de los recursos publicables se generan en el cliente y se envian en minusculas, lo que permite preparar acciones sin conexion.

## Rutas

Todas las rutas siguientes comienzan con `/api`. Los nombres de campos de las peticiones usan camelCase. Las respuestas de lectura del dominio usan los nombres SQL en snake_case, tal como muestran los ejemplos y las pruebas.

| Metodo | Ruta | Uso |
|---|---|---|
| GET | /usuarios/me | Datos propios y roles |
| GET / POST | /catalogos/{tipo} | Leer catalogos o crear una entrada como ADMIN |
| PUT / DELETE | /admin/usuarios/{usuarioId}/roles/{tipo}/{rolId} | Asignar o retirar rol como ADMIN |
| PATCH | /admin/usuarios/{id}/verificar | Verificacion inicial o institucional por ADMIN |
| POST | /usuarios/{id}/avales | Aval de un vecino verificado |
| GET | /perfiles?lat=-16.40&lng=-71.53&tipo=SERVICIO | Cobertura y ranking bayesiano |
| POST | /perfiles | Publicar oferta |
| PATCH | /perfiles/{id}/visibilidad | Pausar o mostrar perfil propio |
| PUT | /perfiles/{id}/voto | Puntuar despues de finalizar una solicitud |
| GET / POST | /solicitudes | Listar solicitudes propias o crear una |
| PATCH | /solicitudes/{id} | Aceptar, rechazar o finalizar |
| GET / POST | /solicitudes/{id}/mensajes | Consultar o enviar mensajes |
| PUT | /solicitudes/{id}/calificacion-vecino | Calificar al cliente como proveedor |
| GET | /muro?zonaId=UUID | Favores abiertos del sector |
| POST | /muro | Crear favor |
| PATCH | /muro/{id}/cerrar | Cerrar publicacion propia |
| GET | /reportes?zonaId=UUID | Reportes vigentes y conteos |
| POST | /reportes | Publicar situacion en lugar publico |
| PUT | /reportes/{id}/respuesta | Confirmar o desmentir |
| GET / POST | /zonas | Listar o crear zonas como ADMIN |
| GET | /centros?zonaId=UUID | Centros publicos |
| POST | /centros | Crear centro como ADMIN |
| POST | /paquetes | Registrar un archivo de mapa como ADMIN |
| GET | /zonas/{id}/paquete | Ultima version del mapa y centros |
| POST | /sync | Aplicar acciones offline sin duplicarlas |

Los valores de `tipo` para catalogos son `categorias`, `rolesComunitarios` y `rolesSistema`. Las rutas de asignacion de roles aceptan los dos ultimos. El rol ADMIN habilita las operaciones de administracion. Otros nombres de rol se guardan para usos comunitarios o futuras reglas de permisos.

Las consultas de perfiles, solicitudes, reportes y muro devuelven hasta 100 filas. Los mensajes admiten `?offset=0`, `?offset=100`, etc. Los catalogos, zonas y centros devuelven hasta 500 filas. Estos limites corresponden a la primera version de la app.

## Ejemplos del dominio

Los UUID siguientes son ilustrativos y deben corresponder a registros reales. Para crear una categoria como ADMIN, envia `{"nombre":"Gasfiteria","descripcion":"Reparaciones de agua"}` a `POST /api/catalogos/categorias`.

Perfil, `POST /api/perfiles`

```json
{
  "id": "11111111-1111-4111-8111-111111111111",
  "categoriaId": "22222222-2222-4222-8222-222222222222",
  "tipo": "SERVICIO",
  "descripcion": "Reparacion de fugas",
  "areaWkt": "MULTIPOLYGON(((-71.56 -16.43,-71.50 -16.43,-71.50 -16.37,-71.56 -16.37,-71.56 -16.43)))"
}
```

La geometria usa longitud y luego latitud, con SRID 4326. El API devuelve cobertura y confianza del proveedor. Los centros y reportes publicos devuelven latitud y longitud.

Solicitud, `POST /api/solicitudes`

```json
{
  "id": "33333333-3333-4333-8333-333333333333",
  "perfilId": "11111111-1111-4111-8111-111111111111",
  "mensaje": "Necesito reparar una fuga"
}
```

El proveedor acepta con `{"estado":"ACEPTADA"}` o rechaza con `{"estado":"RECHAZADA"}`. Cualquiera de los dos participantes puede finalizar una solicitud aceptada con `{"estado":"FINALIZADA"}`.

Mensaje, `POST /api/solicitudes/{id}/mensajes`

```json
{"id":"44444444-4444-4444-8444-444444444444","texto":"Podemos coordinar por aqui"}
```

Voto o calificacion del vecino

```json
{"valor":5}
```

Publicacion del muro

```json
{
  "id":"55555555-5555-4555-8555-555555555555",
  "zonaId":"66666666-6666-4666-8666-666666666666",
  "tipo":"OFREZCO",
  "categoria":"Herramientas",
  "texto":"Presto una escalera"
}
```

Reporte publico

```json
{
  "id":"77777777-7777-4777-8777-777777777777",
  "tipo":"VIA_BLOQUEADA",
  "lat":-16.4,
  "lng":-71.53,
  "texto":"La calle esta bloqueada por escombros",
  "duracionHoras":3
}
```

Respuesta al reporte, `{"decision":"CONFIRMA"}` o `{"decision":"DESMIENTE"}`. La segunda respuesta del mismo vecino actualiza su decision.

## Chat STOMP

Conecta a `ws://localhost:8080/ws` con cabecera STOMP `Authorization: Bearer TOKEN`. Suscribete a `/user/queue/mensajes`. Cada participante recibe sus propios mensajes por esa cola privada.

Envia mensajes por la ruta REST de la solicitud. Tras confirmar la transaccion, el servidor publica el mensaje para el cliente y el proveedor. La base conserva el historial para recuperarlo despues de reconectar. Se rechazan las suscripciones directas a colas ajenas y los envios STOMP a destinos arbitrarios.

## Sincronizacion

`POST /api/sync` recibe hasta 100 acciones. Cada accion se procesa en su propia transaccion. El servidor devuelve APLICADA, YA_APLICADA o ERROR. El cliente mantiene los UUID originales al reintentar y retira de su cola las acciones aplicadas.

```json
{
  "acciones":[
    {
      "idAccion":"88888888-8888-4888-8888-888888888888",
      "tipo":"MURO",
      "muro":{
        "id":"99999999-9999-4999-8999-999999999999",
        "zonaId":"66666666-6666-4666-8666-666666666666",
        "tipo":"OFREZCO",
        "categoria":"Herramientas",
        "texto":"Presto un martillo"
      }
    }
  ]
}
```

| Tipo de accion | Contenido |
|---|---|
| PERFIL | perfil con el cuerpo de creacion |
| SOLICITUD | solicitud con el cuerpo de creacion |
| ESTADO_SOLICITUD | destinoId y estado con estado |
| MENSAJE | destinoId de solicitud y mensaje con id, texto |
| VOTO | destinoId de perfil y voto con valor |
| AVAL | destinoId del vecino |
| CALIFICACION | destinoId de solicitud y voto con valor |
| MURO | muro con el cuerpo de creacion |
| CERRAR_MURO | destinoId de publicacion |
| REPORTE | reporte con el cuerpo de creacion |
| RESPUESTA | destinoId de reporte y respuesta con decision |
| VISIBILIDAD | destinoId de perfil y visibilidad con visible |

Usa /sync tambien para enviar las acciones en linea cuando necesites reintentos idempotentes. El cliente debe enviar exactamente el mismo contenido al repetir un idAccion. Las rutas REST directas de creacion rechazan los UUID duplicados.

El acceso a la cuenta, la verificacion administrativa y los cambios de permisos requieren conexion. El frontend administra IndexedDB, la cola, los reintentos y la descarga de mapas.

## Reglas y decisiones de esta version

- Servicios y apoyo requieren una cuenta verificada para publicar y solicitar. Tiendas admiten registro libre.
- Dos avales de cuentas verificadas y activas verifican al vecino. ADMIN puede verificar las cuentas iniciales.
- El voto al perfil requiere una solicitud finalizada. Esto concreta la recomendacion del documento de vincular el voto a un contacto real. La residencia del vecino no se almacena, asi que no se certifica su pertenencia al barrio.
- El promedio bayesiano de perfiles y clientes usa C=10 y m=3.
- Las calificaciones son de 1 a 5 y se pueden actualizar conservando una por perfil o solicitud.
- El chat permite mensajes nuevos mientras la solicitud esta aceptada. Su historial se elimina siete dias despues de finalizar, mediante una tarea que se ejecuta cada hora.
- Los reportes duran entre una y 24 horas. Su caducidad se aplica en las consultas.
- Se valida la estructura de los reportes. La revision humana del contenido sobre lugares publicos sigue siendo necesaria.
- Los paquetes de mapa se registran mediante una URL HTTPS de un archivo ya generado. La preparacion y alojamiento de PMTiles pertenece a la integracion del mapa.
- Los QR firmados para transferir acciones entre cuentas, el bloqueo de usuarios y la gestion de denuncias quedan pendientes de una ampliacion. El esquema aprobado tampoco incluye sus registros.
- El modo emergencia, el ahorro de bateria, las pantallas y el almacenamiento local corresponden al frontend.
- Se usa JPA para usuarios y consultas SQL parametrizadas con JdbcTemplate para el resto del dominio, incluyendo geometria y bloqueos de concurrencia. Ambas participan en las transacciones de Spring.

## Referencias

[Spring Boot](https://docs.spring.io/spring-boot/4.0/) y [tipos geograficos de MySQL](https://dev.mysql.com/doc/refman/8.4/en/spatial-type-overview.html).

## Organizacion por capas

```text
src/main/java/pe/llaqtamasi/
  Application.java
  controller/   Rutas HTTP, validacion de peticiones y respuestas
  service/      Casos de uso, reglas y transacciones
  repository/   Persistencia JPA y consultas JDBC parametrizadas
  entity/       Entidades persistentes JPA
  dto/          Contratos de entrada y enumeraciones
  security/     Comprobaciones de cuenta, verificacion y permisos
  config/       JWT, CORS, STOMP y administrador inicial
  messaging/    Eventos de chat y limpieza del historial
  exception/    Traduccion de errores a respuestas HTTP
```

Los controladores delegan en los servicios. AuthService gestiona registro y acceso, AdminService la administracion y ConsultaService las lecturas del dominio. CommunityService, SolicitudService y SyncService mantienen las operaciones y sus transacciones.

Usuario es la entidad JPA. El resto del esquema se consulta mediante el repositorio JDBC Db. Las pruebas de reglas estan en src/test/java/pe/llaqtamasi/service.
