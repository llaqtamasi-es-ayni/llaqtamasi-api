# Llaqtamasi es Ayni

Red vecinal de confianza para el Cercado de Arequipa. Conecta a vecinos, servicios y centros de ayuda, y hace visible quién cuenta con el respaldo de su comunidad.

Esta guía instala el sistema completo en una computadora con Windows y recorre sus funciones con datos de prueba. Toma unos 20 minutos, según la conexión.

## Qué permite hacer

| Función | Descripción |
|---|---|
| **Mapa de centros de ayuda** | Comedores, ollas comunes, salud, comisarías y centros de acopio, con filtro por tipo |
| **Cadena de favores** | Avisos de "ofrezco" y "necesito", con conversación privada entre vecinos |
| **Directorio vecinal** | Servicios y locales de la zona, con solicitudes y reputación |
| **Visto Bueno Vecinal** | Una cuenta se verifica con el aval de dos vecinos ya verificados |
| **Reportes comunitarios** | Incidentes con lugar y vigencia, que otros vecinos confirman o desmienten |
| **Modo emergencia** | Tema de fondo negro para toda la aplicación, que ahorra batería en pantallas OLED |

## Componentes

| Componente | Tecnología | Repositorio |
|---|---|---|
| Aplicación | Angular, Ionic, Capacitor | [llaqtamasi-client](https://github.com/llaqtamasi-es-ayni/llaqtamasi-client) |
| API | Java 21, Spring Boot | [llaqtamasi-api](https://github.com/llaqtamasi-es-ayni/llaqtamasi-api) |
| Base de datos | MySQL | `database/` en el repositorio de la API |

## 1. Requisitos

Instala lo siguiente, si aún no lo tienes:

| Programa | Versión | Instalación en PowerShell |
|---|---|---|
| Git | Reciente | `winget install Git.Git` |
| JDK | 21 | `winget install EclipseAdoptium.Temurin.21.JDK` |
| Node.js | LTS | `winget install OpenJS.NodeJS.LTS` |
| MySQL Server | 8.0 o posterior | [Instalador oficial](https://dev.mysql.com/downloads/installer/) |

Durante la instalación de MySQL, anota la contraseña que definas para `root`. Después de instalar, cierra y vuelve a abrir PowerShell.

Esta guía se comprobó en Windows 10 con JDK 21, Node.js 24, MySQL 8.0 y Chrome. El mapa necesita internet.

## 2. Instalación

### 2.1 Descargar el código

```powershell
cd C:\
mkdir llaqtamasi
cd llaqtamasi
git clone -b feature/v2 https://github.com/llaqtamasi-es-ayni/llaqtamasi-api.git
git clone https://github.com/llaqtamasi-es-ayni/llaqtamasi-client.git
```

### 2.2 Crear la base de datos

El archivo `database/datos-prueba.sql` crea la base `llaqtamasi_ayni` con su estructura completa y los datos de prueba. `actualizar-vigencias.sql` renueva los avisos y reportes, que vencen con el tiempo.

```powershell
cd C:\llaqtamasi\llaqtamasi-api
$mysql = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe'
cmd /c "`"$mysql`" -u root -p --binary-mode --default-character-set=utf8mb4 < database\datos-prueba.sql"
cmd /c "`"$mysql`" -u root -p --default-character-set=utf8mb4 < database\actualizar-vigencias.sql"
```

Cada comando pide la contraseña de `root`. Si tienes otra versión de MySQL, ajusta la ruta de `$mysql`.

> **Importante:** si ya existe una base `llaqtamasi_ayni`, sus tablas se reemplazan.

Para comprobarlo, este comando debe responder `7`:

```powershell
& $mysql -u root -p -N -e "SELECT COUNT(*) FROM llaqtamasi_ayni.usuario;"
```

### 2.3 Iniciar la API

En la misma ventana de PowerShell, reemplaza `TU_CLAVE_DE_MYSQL` por tu contraseña de `root`:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\preparar.ps1
.\mvnw.cmd "-Dmaven.repo.local=$PWD\.tools\repository" clean package

$env:DB_USER = 'root'
$env:DB_PASSWORD = 'TU_CLAVE_DE_MYSQL'
$env:JWT_SECRET = 'clave-local-de-evaluacion-de-al-menos-32-bytes'
.\iniciar.ps1
```

La primera compilación descarga dependencias y tarda varios minutos. Si Maven indica que falta `JAVA_HOME`, defínelo con la ruta de tu JDK antes de compilar. Por ejemplo:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
```

La API está lista cuando la ventana muestra `Started Application`. Para comprobarlo, abre <http://localhost:8080/api/salud>, que debe responder `{"estado":"OK"}`. Las demás rutas requieren iniciar sesión, así que en el navegador responden con un error 401.

Deja esta ventana abierta.

### 2.4 Iniciar la aplicación

En una **ventana nueva** de PowerShell:

```powershell
cd C:\llaqtamasi\llaqtamasi-client
npm install
npm start
```

Cuando termine, abre <http://localhost:4200>. Si `npm install` falla por un error de red como `ECONNRESET`, vuelve a ejecutarlo.

### 2.5 Volver a iniciar el sistema

Después de la instalación solo hacen falta dos ventanas de PowerShell:

```powershell
# Ventana 1: API
cd C:\llaqtamasi\llaqtamasi-api
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
$env:DB_USER = 'root'
$env:DB_PASSWORD = 'TU_CLAVE_DE_MYSQL'
$env:JWT_SECRET = 'clave-local-de-evaluacion-de-al-menos-32-bytes'
.\iniciar.ps1
```

```powershell
# Ventana 2: aplicación
cd C:\llaqtamasi\llaqtamasi-client
npm start
```

## 3. Cuentas de prueba

Todas estas cuentas usan la contraseña **`Abc123456789`**:

| Usuario | Nombre visible | Estado | Uso en las pruebas |
|---|---|---|---|
| `admin` | Administracion | Verificada, rol administrador | Atiende las solicitudes del "Taller de Vehiculos" |
| `demo_android` | Administracion | Verificada, rol administrador | Cuenta adicional con historial de solicitudes y votos |
| `luis123` | Lucho Fernandez | Verificada | Da avales, publica avisos, servicios y reportes |
| `lionel123` | Lionel Messi | Sin verificar | Recibe avales hasta verificarse |
| `gus123` | Gustavo Cerati | Sin verificar | Responde avisos y solicita servicios |

Los perfiles "Reparaciones del Cercado" y "Tienda vecinal" pertenecen a cuentas de directorio que no permiten iniciar sesión.

> **Seguridad:** estas credenciales son públicas y sirven solo para evaluar el sistema en una computadora local. No uses estos datos ni el `JWT_SECRET` de esta guía en un servidor accesible desde internet.

## 4. Probar las funciones

Varias pruebas necesitan dos cuentas a la vez. Cada pestaña del navegador mantiene su propia sesión, así que basta con abrir una pestaña por cuenta. Recargar la página cierra la sesión de esa pestaña.

### 4.1 Mapa de centros de ayuda

**Cuenta:** `luis123`

1. Entra a la pestaña **Mapa**. Debes ver cuatro pines. El color indica la familia del centro y el icono, su tipo.
2. En **Tipo de centro**, elige **Salud**. Debe quedar un solo pin y el mensaje "Centros visibles: 1".
3. Vuelve a elegir **Todos** y toca un pin para ver su ficha.

### 4.2 Visto Bueno Vecinal

**Cuentas:** `lionel123`, `luis123` y `admin`

1. Con `lionel123`, abre **Perfil**. Debe decir "Cuenta sin verificar". En **Reportes** aparece "Solo las cuentas verificadas pueden crear reportes."
2. Con `luis123`, en **Perfil**, pulsa **Recomendar vecino**, busca `lionel` y pulsa **Dar aval**.
3. Repite el paso anterior con `admin`.
4. Con `lionel123`, sal de **Perfil** y vuelve a entrar. Ahora debe decir "Cuenta verificada" y ya puede crear reportes.

### 4.3 Cadena de favores

**Cuentas:** `luis123` y `gus123`

1. Con `luis123`, en **Muro**, pulsa **+ Publicar**. Deja "Aviso vecinal", elige **Necesito**, completa el asunto y la descripción, y pulsa **Publicar**.
2. Con `gus123`, en **Muro**, pulsa **Actualizar**, abre el aviso con **Ver aviso** y envía un mensaje. Una cuenta sin verificar también puede responder.
3. Con `luis123`, abre el mismo aviso. En "Respuestas a este aviso", entra a la conversación con Gustavo Cerati y contesta.

Al terminar, usa **← Volver al Muro**: la pestaña Muro conserva abierto el último aviso.

### 4.4 Directorio y reputación

**Cuentas:** `gus123` y `admin`

1. Con `gus123`, en **Muro**, busca "Taller de Vehiculos". Debe mostrar **3.00/5 · Votos: 3**.
2. Pulsa **Solicitar**, escribe un mensaje y pulsa **Enviar solicitud**. Una cuenta sin verificar puede solicitar a una tienda, pero no a un servicio.
3. Con `admin`, en **Perfil**, pulsa **Actualizar** en "Mis solicitudes". En la solicitud recibida, pulsa **Aceptar** y luego **Finalizar**.
4. Con `admin`, elige una nota en **Calificar al vecino** y pulsa **Guardar calificación**.
5. Con `gus123`, en **Perfil**, pulsa **Actualizar**, elige una nota en **Calificar perfil** y pulsa **Guardar voto**.
6. En **Muro**, pulsa **Actualizar**. La reputación cambia: con 5 estrellas, pasa a **3.14/5 · Votos: 4**.

La reputación empieza en 3 y se calcula como `(30 + suma de estrellas) / (10 + número de votos)`, así que un solo voto no la altera demasiado.

### 4.5 Publicar un servicio

**Cuentas:** `luis123` y `gus123`

1. Con `luis123`, en **Muro**, pulsa **+ Publicar** y elige **Servicio calificable**.
2. Elige la categoría, escribe la descripción y pulsa **Publicar**.
3. Con `gus123`, en **Muro**, pulsa **Actualizar**. El servicio debe aparecer con la opción **Solicitar**.

Solo las cuentas verificadas pueden publicar servicios.

### 4.6 Reportes comunitarios

**Cuentas:** `luis123`, `gus123` y `admin`

1. Con `luis123`, en **Reportes**, pulsa **+ Nuevo reporte**, pulsa **Elegir en el mapa** y toca un lugar. Debe aparecer "Punto seleccionado."
2. Escribe la descripción y pulsa **Publicar**. El autor no ve las opciones para confirmar su propio reporte.
3. Con `gus123`, en **Reportes**, pulsa **Actualizar** y luego **Confirmar**. El contador de confirmaciones sube.
4. Con `admin`, en **Reportes**, pulsa **Actualizar** y luego **Desmentir**. El contador de desmentidos sube.

### 4.7 Modo emergencia

**Cuenta:** cualquiera

1. En **Reportes**, pulsa **Activar modo emergencia**. La aplicación cambia a fondo negro.
2. Recorre **Mapa**, **Muro** y **Perfil**: el modo se mantiene en todas las pantallas y el mapa también se oscurece.
3. Recarga la página: el modo sigue activo, incluso en el inicio de sesión.
4. Inicia sesión y, en **Reportes**, pulsa **Desactivar modo emergencia**.

El modo se guarda en el navegador, así que afecta a todas las pestañas al recargarlas.

## 5. Restablecer los datos

Las pruebas modifican la base. Para volver al estado inicial, repite los dos comandos de MySQL de la sección [2.2](#22-crear-la-base-de-datos) y reinicia la API.

Si un aviso o un reporte de prueba ya venció, basta con ejecutar de nuevo `actualizar-vigencias.sql`.

## 6. Solución de problemas

| Síntoma | Qué revisar |
|---|---|
| La pantalla de inicio de sesión indica que la API no está activa | Que la ventana de la API siga abierta y que <http://localhost:8080/api/salud> responda |
| "El usuario o la contraseña son incorrectos" | Que uses un usuario de la [sección 3](#3-cuentas-de-prueba) y que la base provenga de `datos-prueba.sql` |
| La API se detiene al arrancar | Que MySQL esté activo, que `DB_PASSWORD` sea correcta y que el puerto 8080 esté libre |
| `iniciar.ps1` no se ejecuta | Ejecuta antes `Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass` |
| No aparecen avisos o reportes | Ejecuta `actualizar-vigencias.sql` y pulsa **Actualizar** en la pantalla |
| El mapa se ve en gris | Comprueba la conexión a internet |

## 7. Alcance actual y próximos pasos

Las siguientes capacidades aún no están implementadas:

- Mostrar los reportes en el mapa principal; hoy aparecen en la pestaña Reportes.
- Dibujar en el mapa las áreas de atención de los servicios y permitir que cada proveedor elija la suya; hoy cubren la zona completa.
- Mapa y centros de ayuda disponibles sin conexión.
- Pantallas de administración para zonas, centros y categorías; hoy se gestionan mediante la API.

---

# Referencia técnica de la API

La API queda en `http://localhost:8080/api` y el WebSocket en `ws://localhost:8080/ws`.

Variables de entorno:

| Variable | Obligatoria | Descripción |
|---|---|---|
| `DB_USER`, `DB_PASSWORD` | Sí | Credenciales de MySQL |
| `JWT_SECRET` | Sí | Clave de firma de los tokens, de al menos 32 bytes. Mantenla entre reinicios para que los tokens emitidos sigan siendo válidos |
| `DB_URL` | No | Conexión JDBC. Por defecto, `llaqtamasi_ayni` en `localhost:3306` |
| `PORT` | No | Puerto de la API. Por defecto, `8080` |
| `CORS_ORIGINS` | No | Orígenes permitidos. Por defecto, `http://localhost:4200,http://localhost:8100` |
| `ADMIN_LOGIN`, `ADMIN_PASSWORD` | No | Crean una cuenta administradora al arrancar, si ese usuario aún no existe. La contraseña debe tener 12 caracteres o más |

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
