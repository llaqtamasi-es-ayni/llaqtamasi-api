import json
import os
import urllib.request
import urllib.error
import uuid
from concurrent.futures import ThreadPoolExecutor

BASE = os.environ.get("API_URL", "http://localhost:18080") + "/api"
def uid():
    return str(uuid.uuid4())

def call(method, path, body=None, token=None, expected=200):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    request = urllib.request.Request(BASE + path,
        data=json.dumps(body).encode() if body is not None else None,
        headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            status, raw = response.status, response.read()
    except urllib.error.HTTPError as error:
        status, raw = error.code, error.read()
    assert status == expected, (method, path, status, raw.decode())
    return json.loads(raw) if raw else None

admin = call("POST", "/auth/login", {
    "nombreAcceso": os.environ["ADMIN_LOGIN"],
    "password": os.environ["ADMIN_PASSWORD"]})["accessToken"]
suffix = uuid.uuid4().hex[:10]
def register(name):
    return call("POST", "/auth/registro", {
        "nombreAcceso": name + suffix, "alias": name,
        "password": "Clave-prueba-segura-2026"}, expected=201)

provider = register("proveedor")
client = register("cliente")
outsider = register("externo")
pt, ct, ot = [x["accessToken"] for x in (provider, client, outsider)]
for person in (provider, client):
    call("PATCH", "/admin/usuarios/" + person["usuarioId"] + "/verificar", token=admin)
call("GET", "/usuarios/me", token=ct)
call("GET", "/usuarios/me", expected=401)
category = call("POST", "/catalogos/categorias", {"nombre": "Gasfiteria " + suffix}, admin)["id"]
call("POST", "/catalogos/categorias", {"nombre": "Prohibida"}, ct, expected=403)
zone = uid()
area = "MULTIPOLYGON(((-71.56 -16.43,-71.50 -16.43,-71.50 -16.37,-71.56 -16.37,-71.56 -16.43)))"
call("POST", "/zonas", {"id": zone, "nombre": "Zona de prueba " + suffix, "limiteWkt": area}, admin)
profile = uid()
call("POST", "/perfiles", {"id": profile, "categoriaId": category,
    "tipo": "SERVICIO", "descripcion": "Gasfiteria de prueba", "areaWkt": area}, pt)
rows = call("GET", "/perfiles?lat=-16.40&lng=-71.53", token=ct)
assert any(row["id"] == profile for row in rows)
rows = call("GET", "/perfiles?lat=-12&lng=-77", token=ct)
assert not any(row["id"] == profile for row in rows)
request_id = uid()
call("POST", "/solicitudes", {"id": request_id, "perfilId": profile, "mensaje": "Reparar una fuga"}, ct)
call("PATCH", "/solicitudes/" + request_id, {"estado": "ACEPTADA"}, ct, expected=403)
call("POST", "/solicitudes/" + request_id + "/mensajes",
    {"id": uid(), "texto": "Antes de aceptar"}, ct, expected=400)
call("PATCH", "/solicitudes/" + request_id, {"estado": "ACEPTADA"}, pt)
call("POST", "/solicitudes/" + request_id + "/mensajes", {"id": uid(), "texto": "Coordinamos aqui"}, ct)
messages = call("GET", "/solicitudes/" + request_id + "/mensajes", token=pt)
assert len(messages) == 1
call("GET", "/solicitudes/" + request_id + "/mensajes", token=ot, expected=403)
call("PATCH", "/solicitudes/" + request_id, {"estado": "FINALIZADA"}, ct)
call("PUT", "/perfiles/" + profile + "/voto", {"valor": 5}, ct)
call("PUT", "/perfiles/" + profile + "/voto", {"valor": 6}, ct, expected=400)
call("PUT", "/solicitudes/" + request_id + "/calificacion-vecino", {"valor": 4}, pt)
call("POST", "/usuarios/" + outsider["usuarioId"] + "/avales", token=pt)
call("POST", "/usuarios/" + outsider["usuarioId"] + "/avales", token=ct)
assert call("GET", "/usuarios/me", token=ot)["verificado"]
report = uid()
call("POST", "/reportes", {"id": report, "tipo": "VIA_BLOQUEADA",
    "lat": -16.40, "lng": -71.53, "texto": "Situacion publica de prueba", "duracionHoras": 2}, ct)
call("PUT", "/reportes/" + report + "/respuesta", {"decision": "CONFIRMA"}, pt)
call("PUT", "/reportes/" + report + "/respuesta", {"decision": "DESMIENTE"}, pt)
rows = call("GET", "/reportes?zonaId=" + zone, token=ct)
row = next(r for r in rows if r["id"] == report)
assert row["confirmaciones"] == 0 and row["desmentidos"] == 1
action = {"idAccion": uid(), "tipo": "MURO", "muro": {
    "id": uid(), "zonaId": zone, "tipo": "OFREZCO", "categoria": "Herramientas", "texto": "Presto martillo"}}
def sync():
    return call("POST", "/sync", {"acciones": [action]}, ct)[0]["estado"]
with ThreadPoolExecutor(max_workers=2) as pool:
    results = list(pool.map(lambda _: sync(), range(2)))
assert sorted(results) == ["APLICADA", "YA_APLICADA"], results
assert sync() == "YA_APLICADA"
retry_action = {"idAccion": uid(), "tipo": "MURO", "muro": {
    "id": uid(), "zonaId": uid(), "tipo": "OFREZCO", "categoria": "Prueba", "texto": "Zona inexistente"}}
assert call("POST", "/sync", {"acciones": [retry_action]}, ct)[0]["estado"] == "ERROR"
retry_action["muro"]["zonaId"] = zone
assert call("POST", "/sync", {"acciones": [retry_action]}, ct)[0]["estado"] == "APLICADA"
center = uid()
call("POST", "/centros", {"id": center, "zonaId": zone, "nombre": "Centro de prueba",
    "tipo": "ACOPIO", "lat": -16.4, "lng": -71.53}, admin)
assert any(c["id"] == center for c in call("GET", "/centros?zonaId=" + zone, token=ct))
call("POST", "/paquetes", {"id": uid(), "zonaId": zone, "version": 1,
    "referenciaArchivo": "https://example.org/mapa-prueba.pmtiles"}, admin)
assert call("GET", "/zonas/" + zone + "/paquete", token=ct)["paquete"]["version"] == 1
role = call("POST", "/catalogos/rolesComunitarios", {"nombre": "Voluntario " + suffix}, admin)["id"]
call("PUT", "/admin/usuarios/" + client["usuarioId"] + "/roles/rolesComunitarios/" + role, token=admin)
assert any(r["id"] == role for r in call("GET", "/usuarios/me", token=ct)["rolesComunitarios"])
print("OK: registro, permisos, geografia, solicitudes, chat REST, votos, avales, reportes, roles, paquetes y sync concurrente.")

# Minimal RFC 6455 client using only the Python standard library.
import socket
import base64
import struct

class WebSocket:
    def __init__(self, token):
        from urllib.parse import urlsplit
        address = urlsplit(BASE)
        self.socket = socket.create_connection((address.hostname, address.port or 80), timeout=10)
        key = base64.b64encode(os.urandom(16)).decode()
        handshake = ("GET /ws HTTP/1.1\r\nHost: " + address.netloc +
            "\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Key: " + key +
            "\r\nSec-WebSocket-Version: 13\r\nOrigin: http://localhost:4200\r\n\r\n")
        self.socket.sendall(handshake.encode())
        response = b""
        while not response.endswith(b"\r\n\r\n"):
            response += self.socket.recv(1)
        assert b"101" in response.split(b"\r\n")[0], response
        self.send("CONNECT\naccept-version:1.2\nhost:localhost\nAuthorization:Bearer " + token + "\n\n\x00")
        assert self.receive().startswith("CONNECTED")

    def read(self, size):
        result = b""
        while len(result) < size:
            part = self.socket.recv(size - len(result))
            if not part:
                raise RuntimeError("WebSocket cerrado")
            result += part
        return result

    def send(self, text):
        payload = text.encode()
        mask = os.urandom(4)
        size = len(payload)
        header = bytes([0x81, 0x80 | size]) if size < 126 else bytes([0x81, 0xfe]) + struct.pack("!H", size)
        self.socket.sendall(header + mask + bytes(value ^ mask[i % 4] for i, value in enumerate(payload)))

    def receive(self):
        first, second = self.read(2)
        size = second & 127
        if size == 126:
            size = struct.unpack("!H", self.read(2))[0]
        elif size == 127:
            size = struct.unpack("!Q", self.read(8))[0]
        return self.read(size).decode()

    def close(self):
        self.socket.close()

chat_request = uid()
call("POST", "/solicitudes", {"id": chat_request, "perfilId": profile, "mensaje": "Probar chat en vivo"}, ct)
call("PATCH", "/solicitudes/" + chat_request, {"estado": "ACEPTADA"}, pt)
connection = WebSocket(pt)
try:
    connection.send("SUBSCRIBE\nid:chat\ndestination:/user/queue/mensajes\n\n\x00")
    import time
    time.sleep(0.2)
    call("POST", "/solicitudes/" + chat_request + "/mensajes", {"id": uid(), "texto": "Mensaje en vivo"}, ct)
    frame = connection.receive()
    assert frame.startswith("MESSAGE") and "Mensaje en vivo" in frame, frame
finally:
    connection.close()
intruder = WebSocket(ot)
try:
    intruder.send("SUBSCRIBE\nid:intruso\ndestination:/queue/mensajes\n\n\x00")
    assert intruder.receive().startswith("ERROR")
finally:
    intruder.close()
print("OK: entrega WebSocket/STOMP y rechazo de suscripcion a cola ajena.")
