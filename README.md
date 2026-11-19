# WePay · Microservicio de Reportería

**Repositorio:** [`WePayCloudNative/wepay-ms-reportes`](https://github.com/WePayCloudNative/wepay-ms-reportes)  
**Proyecto:** WePay / Pedidos360 · Desarrollo Cloud Native I  
**Estado:** funcionalidad principal validada localmente; persistencia Oracle e integración en AWS pendientes de validar.

## Descripción

`wepay-ms-reportes` es el microservicio de análisis y reportería de WePay. Consume eventos de pedidos publicados en Apache Kafka, mantiene una proyección local de pedidos y sus productos, y expone una API REST para construir un dashboard administrativo con indicadores de ventas y operación.

El servicio **no crea ni modifica pedidos en el microservicio transaccional**: interpreta eventos y mantiene una vista de lectura para consultas analíticas. Otros componentes del sistema, como BFF, frontend, autenticación y despliegue cloud, pertenecen a la solución WePay, pero no se implementan en este repositorio.

## Tecnologías

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 25 |
| Framework | Spring Boot 4.1.2 |
| API REST | Spring Web MVC |
| Mensajería | Apache Kafka / Spring Kafka |
| Persistencia | Spring Data JPA / Hibernate |
| Base local | H2 en memoria |
| Base prevista para producción | Oracle Database |
| Driver Oracle declarado | `ojdbc17` y `ojdbc11` (ver observaciones) |
| Compilación | Maven Wrapper (`mvnw`, `mvnw.cmd`) |
| Puerto HTTP | `8087` |

## Arquitectura y flujo

```text
wepay-ms-pedidos (productor de eventos)
            |
            v
        Apache Kafka
        orders.events (3 particiones en las pruebas locales)
            |
            v
   PedidoEventoConsumer
            |
            v
   PedidoReporteService
       |          |
       v          v
 pedidos_reporte  eventos_procesados_reporte
       |
       v
 detalle_pedido_reporte
       |
       v
    ReporteService
       |
       v
    ReporteController
       |
       v
 GET /api/report/*  --> BFF / dashboard (integración pendiente)

Error persistente al consumir:
orders.events --> reintentos --> orders.events.DLT
```

**Tópicos Kafka:**

| Tópico | Uso |
|---|---|
| `orders.events` | Eventos originales del ciclo de vida del pedido |
| `orders.events.DLT` | Mensajes que no pudieron procesarse tras los reintentos |

El consumidor pertenece al grupo `wepay-reportes-group`. La configuración actual reintenta **dos veces adicionales**, con espera de **1 segundo** entre intentos, y luego publica el registro original en `orders.events.DLT`, conservando su número de partición. Por ello, ambos tópicos deben contar con particiones compatibles.

> En las pruebas locales se utilizó un broker Kafka y tópicos con tres particiones y factor de replicación **1**. Un clúster de tres brokers con factor de replicación **3** es un requisito de infraestructura que **no está validado por este repositorio**.

## Funcionalidades implementadas

- **KPIs generales:** pedidos registrados, entregados y activos, ventas de pedidos entregados, lead time promedio en minutos y conteo por estado.
- **Top de productos:** cantidades vendidas e ingresos por producto de pedidos entregados.
- **Ventas por hora:** suma de ventas y cantidad de pedidos entregados, agrupadas según la hora de entrega.
- **Idempotencia básica:** registro de identificadores `eventId` en una tabla con clave primaria para ignorar eventos repetidos.
- **Protección básica frente a eventos antiguos:** `fechaUltimoEvento` evita que un evento con fecha anterior sobrescriba el estado más reciente. Un evento `OrderCreated` antiguo todavía puede recuperar los detalles faltantes.
- **Gestión de errores:** los fallos de procesamiento se propagan a Spring Kafka, que aplica reintentos y envía eventos irrecuperables al DLT.

**Limitaciones conocidas:** el orden se compara por `fechaEvento`, sin una secuencia estricta por pedido para empates o concurrencia; la verificación `existsById` no evita por sí sola una carrera entre consumidores; el proyecto utiliza `Double` para montos monetarios; los cálculos actuales usan `repository.findAll()` y filtran en memoria. Estas decisiones son suficientes para las pruebas funcionales realizadas, pero conviene revisarlas para producción y grandes volúmenes.

## Requisitos para ejecutar localmente

1. **JDK 25** instalado (`java -version`).
2. **Docker Desktop** o un broker Kafka accesible.
3. Puerto **8087** disponible para la API.
4. Puerto **9092** accesible desde el equipo para Kafka, o configurar `KAFKA_BOOTSTRAP_SERVERS`.

No se requiere Oracle para levantar el perfil local.

## Configuración por perfiles

| Archivo | Función |
|---|---|
| `src/main/resources/application.yml` | Puerto, nombre del servicio, propiedades Kafka y perfil predeterminado `local` |
| `src/main/resources/application-local.yml` | H2 `jdbc:h2:mem:reportesdb`, usuario `sa`, Hibernate `ddl-auto: update` |
| `src/main/resources/application-prod.yml` | Conexión Oracle por variables de entorno y Hibernate `ddl-auto: validate` |

El perfil `local` se selecciona por defecto. H2 es **volátil**: los datos se pierden al detener el proceso. Los offsets del grupo Kafka pueden mantenerse, por lo que no se debe asumir que los eventos previos se reproducirán después de reiniciar H2.

### Variables de entorno para producción

| Variable | Descripción |
|---|---|
| `SPRING_PROFILES_ACTIVE` | Debe ser `prod` en el despliegue Oracle |
| `DB_URL` | URL JDBC de la base Oracle |
| `DB_USERNAME` | Usuario/esquema de reportería |
| `DB_PASSWORD` | Contraseña de ese usuario |
| `KAFKA_BOOTSTRAP_SERVERS` | Lista de brokers Kafka alcanzables desde el servicio |

No se deben subir contraseñas, wallets, credenciales ni archivos secretos al repositorio. El perfil `prod` **no ha sido verificado aún con una instancia Oracle real**.

## Inicio rápido (Windows / PowerShell)

Desde la raíz del repositorio:

```powershell
java -version
.\mvnw.cmd clean compile
.\mvnw.cmd spring-boot:run
```

La aplicación quedará en `http://localhost:8087` cuando Kafka esté disponible. Para compilar el paquete:

```powershell
.\mvnw.cmd clean package -DskipTests
```

> En Linux/macOS se usa `./mvnw` en lugar de `.\\mvnw.cmd`.

### Kafka local

Este repositorio no contiene necesariamente el `docker-compose.yml` de la infraestructura; el broker utilizado durante el desarrollo se levantó desde el repositorio de orquestación. Con el contenedor local llamado `kafka`, se puede comprobar los tópicos mediante:

```powershell
docker exec kafka kafka-topics --bootstrap-server kafka:29092 --describe --topic orders.events
docker exec kafka kafka-topics --bootstrap-server kafka:29092 --describe --topic orders.events.DLT
```

Si el DLT aún no existe y solo hay **un broker local**:

```powershell
docker exec kafka kafka-topics --bootstrap-server kafka:29092 --create --if-not-exists --topic orders.events.DLT --partitions 3 --replication-factor 1
```

No uses factor de replicación 1 como configuración objetivo de producción con tres brokers.

## API REST

Base: `http://localhost:8087/api/report`

| Método | Endpoint | Rango aceptado | Descripción |
|---|---|---|---|
| `GET` | `/kpis?range=last24h` | `last24h` | KPIs y conteo por estado |
| `GET` | `/top-products?range=last7d` | `last7d` | Productos vendidos, ordenados por cantidad descendente |
| `GET` | `/sales-by-hour?range=last24h` | `last24h` | Ventas y pedidos entregados por hora |

El parámetro `range` es opcional en los tres endpoints; cada uno aplica su rango predeterminado. Un rango distinto del admitido devuelve **HTTP 400**.

### 1. KPIs

```powershell
curl.exe "http://localhost:8087/api/report/kpis?range=last24h"
```

Ejemplo observado durante la prueba de un pedido entregado:

```json
{
  "totalPedidos": 1,
  "pedidosEntregados": 1,
  "pedidosActivos": 0,
  "ventasTotales": 89990.0,
  "leadTimePromedioMinutos": 10.0,
  "pedidosPorEstado": { "ENTREGADO": 1 }
}
```

**Criterio de cálculo:** el conjunto de KPIs se filtra por `fechaCreacion` dentro de las últimas 24 horas; `ventasTotales` suma `total` de pedidos con estado `ENTREGADO`; el lead time promedio se calcula como minutos transcurridos entre creación y entrega, descartando duraciones negativas.

### 2. Productos más vendidos

```powershell
curl.exe "http://localhost:8087/api/report/top-products?range=last7d"
```

```json
[
  {
    "productoId": 50,
    "nombreProducto": "Monitor gamer",
    "cantidadVendida": 1,
    "ingresosTotales": 89990.0
  }
]
```

**Criterio de cálculo:** pedidos en `ENTREGADO` con `fechaCreacion` dentro de los últimos 7 días, agrupados por `productoId`, sumando `cantidad` y `precioUnitario * cantidad`.

### 3. Ventas por hora

```powershell
curl.exe "http://localhost:8087/api/report/sales-by-hour?range=last24h"
```

```json
[
  {
    "hora": "2026-11-19T16:00",
    "cantidadPedidos": 1,
    "totalVentas": 89990.0
  }
]
```

**Criterio de cálculo:** pedidos `ENTREGADO` cuya `fechaEntrega` está dentro de las últimas 24 horas, agrupados por la fecha/hora truncada a la hora. Solo aparecen las horas con ventas; no se generan intervalos vacíos.

> Los ejemplos son respuestas de pruebas locales del **19 de noviembre de 2026**, no datos reales de negocio.

## Contrato de eventos Kafka

El consumidor recibe valores JSON en `orders.events` y los convierte a `PedidoEventoDTO`. Campos utilizados:

| Campo | Tipo | Uso |
|---|---|---|
| `eventId` | String | Clave para deduplicación; requerido |
| `tipoEvento` | String | Tipo de evento, por ejemplo `OrderCreated` y `OrderDelivered` |
| `pedidoId` | Long | Identificador del pedido; requerido |
| `usuarioOid` | String | Identificador del usuario |
| `total` | Double | Total del pedido |
| `estado` | String | Estado reportado |
| `fechaCreacion` | LocalDateTime | Fecha original del pedido |
| `fechaEvento` | LocalDateTime | Orden temporal; requerido |
| `detalles` | Lista | Productos del pedido |

Cada detalle puede incluir `productoId`, `nombreProducto`, `precioUnitario` y `cantidad`.

Para conservar el orden relativo de los eventos del mismo pedido, **se recomienda usar `pedidoId` como clave Kafka**. Kafka preserva el orden por partición, pero no entre particiones distintas.

### Ejemplo: pedido válido

Abre el productor en una consola independiente:

```powershell
docker exec -it kafka kafka-console-producer --bootstrap-server kafka:29092 --topic orders.events --property parse.key=true --property key.separator=:
```

Pega **una línea por mensaje**, sin líneas vacías (la clave precede los dos puntos):

```text
960:{"eventId":"readme-960-created","tipoEvento":"OrderCreated","pedidoId":960,"usuarioOid":"usuario-demo","total":89990.0,"estado":"CREADO","fechaCreacion":"2026-11-19T16:10:00","fechaEvento":"2026-11-19T16:10:00","detalles":[{"productoId":50,"nombreProducto":"Monitor gamer","precioUnitario":89990.0,"cantidad":1}]}
960:{"eventId":"readme-960-delivered","tipoEvento":"OrderDelivered","pedidoId":960,"usuarioOid":"usuario-demo","total":89990.0,"estado":"ENTREGADO","fechaCreacion":"2026-11-19T16:10:00","fechaEvento":"2026-11-19T16:20:00","detalles":[]}
```

Estas fechas son **ejemplos históricos**; para probar rangos móviles en otra fecha deben actualizarse a fechas recientes.

### Ejemplo: mensaje inválido y DLT

El siguiente evento es inválido porque `pedidoId` es `null`:

```json
{"eventId":"readme-dlt-001","tipoEvento":"OrderCreated","pedidoId":null,"usuarioOid":"usuario-demo","total":49990.0,"estado":"CREADO","fechaCreacion":"2026-11-19T16:00:00","fechaEvento":"2026-11-19T16:00:00","detalles":[]}
```

Para observar el DLT:

```powershell
docker exec -it kafka kafka-console-consumer --bootstrap-server kafka:29092 --topic orders.events.DLT --from-beginning
```

El evento inválido debe ser redirigido al DLT después de agotar reintentos. Durante las pruebas se verificó su publicación y que el servicio siguió procesando posteriormente un pedido correcto. **El DLT admite duplicados**: dos publicaciones del mismo evento en el tópico de origen pueden generar dos mensajes en el DLT.

## Modelo de datos

| Entidad JPA | Tabla | Función |
|---|---|---|
| `PedidoReporte` | `pedidos_reporte` | Pedido, usuario, total, estado, fechas y relación con detalles |
| `DetallePedidoReporte` | `detalle_pedido_reporte` | Producto, precio, cantidad y clave foránea al pedido |
| `EventoProcesadoReporte` | `eventos_procesados_reporte` | `eventId`, pedido, tipo de evento y fecha de procesamiento |

Los detalles se relacionan con el pedido usando `@ManyToOne`/`@OneToMany`. El `id` del detalle se genera mediante `GenerationType.IDENTITY`. El registro del evento procesado y la actualización del pedido ocurren en una transacción `@Transactional`.

### Script Oracle

Ruta: [`database/schema-oracle-reportes.sql`](database/schema-oracle-reportes.sql).

El script prepara un esquema Oracle nuevo con tablas, claves primarias, relación foránea e índice por `pedido_id`. **Está guardado como referencia y NO se ha ejecutado en Oracle**. No ejecutarlo sobre un esquema existente sin revisar antes sus objetos y privilegios. El perfil `prod` configura `ddl-auto: validate`: comprueba las tablas existentes, pero **no las crea**.

## Estructura principal del repositorio

```text
wepay-ms-reportes/
├── database/
│   └── schema-oracle-reportes.sql
├── src/main/java/cl/duoc/wepay_ms_reportes/
│   ├── config/
│   │   └── KafkaErrorConfig.java
│   ├── consumer/
│   │   └── PedidoEventoConsumer.java
│   ├── controller/
│   │   └── ReporteController.java
│   ├── dto/
│   │   ├── PedidoEventoDTO.java
│   │   ├── DetalleEventoDTO.java
│   │   ├── KpiResponseDTO.java
│   │   ├── TopProductoDTO.java
│   │   └── VentasPorHoraDTO.java
│   ├── entity/
│   │   ├── PedidoReporte.java
│   │   ├── DetallePedidoReporte.java
│   │   └── EventoProcesadoReporte.java
│   ├── repository/
│   │   ├── PedidoReporteRepository.java
│   │   └── EventoProcesadoReporteRepository.java
│   └── service/
│       ├── PedidoReporteService.java
│       └── ReporteService.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-local.yml
│   └── application-prod.yml
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md
```

## Pruebas funcionales realizadas

| Prueba | Resultado local |
|---|---|
| Inicio de Spring Boot / conexión Kafka | Correcto, puerto 8087 y particiones asignadas |
| `GET /api/report/kpis` | HTTP 200 y métricas esperadas |
| `GET /api/report/top-products` | Agregación de productos correcta |
| `GET /api/report/sales-by-hour` | Agrupación horaria correcta |
| Duplicado de `eventId` | Segundo evento ignorado en logs |
| Eventos fuera de orden | `OrderDelivered` seguido de eventos antiguos conservó `ENTREGADO` |
| Recuperación de productos tardíos | `OrderCreated` antiguo incorporó detalles faltantes |
| Error por `pedidoId` nulo | Publicación observada en `orders.events.DLT` |
| Evento válido después de un fallo | Procesamiento y reportes correctos |
| Perfil `local` | Inicio confirmado con H2 |
| Oracle real / reinicio con persistencia | **No probado** |
| BFF / Angular / AWS | **No probado desde este repositorio** |

En una prueba local del pedido `950`, los tres endpoints devolvieron: **1 pedido entregado, $89.990 en ventas, 10 minutos de lead time, 1 monitor vendido y $89.990 a las 16:00**.

## Observaciones y trabajo pendiente

1. **Oracle en AWS:** aprovisionar la base, revisar permisos, ejecutar/migrar el esquema de forma controlada, activar `prod`, validar Hibernate y comprobar persistencia tras reiniciar.
2. **Dependencias JDBC:** el `pom.xml` actualmente declara **ambos** drivers Oracle `ojdbc17` y `ojdbc11`. Revisar compatibilidad y conservar **solo uno** antes del despliegue; evitar duplicación de controladores en el classpath.
3. **BFF/frontend:** integrar y proteger los endpoints de reportería a través de los componentes correspondientes de WePay. El microservicio por sí solo no implementa el dashboard Angular ni la autenticación.
4. **Kafka productivo:** verificar clúster de tres brokers, replicación, disponibilidad y configuración del DLT según la infraestructura final.
5. **Resiliencia:** mejorar manejo de simultaneidad, secuencia de eventos, política de errores no recuperables y recuperación/reconstrucción de proyecciones.
6. **Rendimiento y precisión:** trasladar agregaciones a consultas de base de datos cuando el volumen aumente; evaluar `BigDecimal` para dinero, zona horaria y coherencia de rangos.
7. **Pruebas automatizadas:** incorporar cobertura para controladores, agregación, idempotencia, eventos retrasados y recuperación a DLT.

## Compilación y solución de problemas

- **`HTTP 200` con contadores en cero:** generalmente no hay pedidos cargados en H2. Si se reinició la aplicación, la base en memoria se vació.
- **El consumidor no conecta:** revisar contenedor Kafka, `localhost:9092` y `KAFKA_BOOTSTRAP_SERVERS`.
- **Mensaje inválido no llega al DLT:** comprobar que se cargue `KafkaErrorConfig` (`CONFIGURACION KAFKA DLT ACTIVADA`), que el listener propague excepciones y que exista `orders.events.DLT` con particiones compatibles.
- **Errores de rango:** solo se admiten los rangos detallados en la tabla de endpoints; otros devuelven `400 Bad Request`.
- **Inicio `prod` falla por tablas faltantes:** `ddl-auto: validate` no ejecuta el SQL automáticamente; validar esquema Oracle y credenciales.

## Contexto del proyecto

Microservicio desarrollado como componente de **WePay / Pedidos360** para una evaluación de **Desarrollo Cloud Native I**. Su responsabilidad concreta es la reportería basada en eventos; los repositorios y servicios de pedidos, auditoría, orquestación, BFF y frontend se gestionan por separado dentro de la solución.

---

**Última actualización de esta documentación:** 19 de noviembre de 2026.  
**Alcance de verificación:** código revisado en GitHub y pruebas locales proporcionadas durante el desarrollo. La ejecución del esquema Oracle y el despliegue AWS siguen pendientes.
