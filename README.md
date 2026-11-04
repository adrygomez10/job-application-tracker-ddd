# Job Application Tracker · DDD y Clean Architecture

Tercera evolución del gestor de candidaturas con Java, Spring Boot, Thymeleaf y PostgreSQL. Parte del repositorio local `job-application-tracker-modular` (`823b95e`), cuyo historial disponible se conserva. El proyecto anterior permanece intacto. Repositorio del taller: https://github.com/adrygomez10/job-application-tracker-ddd.

## Funcionalidades

Se conservan gestión de candidaturas, entrevistas, tareas, historial, CSV con previsualización, exportadores CSV/ICS, recordatorios internos y estadísticas. Se añaden:

- Oportunidades con empresa contratante y agencia distintas, salario bruto por moneda/periodo y datos revisables con versión.
- Procesos sucesivos para una oportunidad y protección persistente frente a dos procesos activos sobre su identidad confirmada.
- Revisión explícita de identidades de candidaturas antiguas/CSV, con versión y auditoría, conservando notas e historial.
- Perfil personal con puesto, ubicación, modalidad, objetivo salarial, consentimiento externo y horas de silencio por zona.
- Advertencias explicables de compatibilidad; importación manual o programada de un tablón Greenhouse sin sobrescribir correcciones propias, con historial de ejecuciones.
- Cola persistente de avisos ntfy, manuales o automáticos, con silencio, categorías, caducidad y resultados conservados sin repetir intentos inciertos.
- Guardado selectivo de colecciones, pruebas de arquitectura, concurrencia con PostgreSQL, migración V3 y navegador.

## Primer arranque

Necesitas Docker con Compose v2 y el motor activo. Java y Maven están incluidos en las imágenes.

PowerShell:

```powershell
.\scripts\setup.ps1
docker compose up -d --wait
```

Linux, macOS o Git Bash:

```bash
bash scripts/setup.sh
docker compose up -d --wait
```

El asistente pide usuario y contraseña; la contraseña debe tener al menos 12 caracteres y como máximo 72 bytes UTF-8. Crea `.env` y nunca sobrescribe una configuración existente. Si ya está configurado, usa directamente el segundo comando.

Abre **http://localhost:8092**. La base empieza vacía y conserva los datos en un volumen propio del proyecto Compose `job-application-tracker-ddd`. PostgreSQL no publica un puerto en el equipo. Cambia APP_PORT y TRACKER_ZONE en tu `.env` si lo necesitas. No compartas volumen ni base con versiones anteriores.

| Operación | Comando |
| --- | --- |
| Estado | `docker compose ps` |
| Registros | `docker compose logs --follow --tail=100 app` |
| Detener conservando datos | `docker compose down` |
| Reconstruir tras cambiar código/configuración | `docker compose up -d --build --wait` |
| Copia | `.\scripts\backup.ps1` o `bash scripts/backup.sh` |

## Recorrido

1. Configura Mi perfil; deja avisos externos desactivados durante el primer recorrido.
2. Crea una oportunidad con salario anual y compara las advertencias con tus preferencias.
3. Registra una candidatura enviada hoy. El segundo proceso activo para esa misma oportunidad se rechaza; cerrar el anterior permite uno nuevo. Archivar no libera la identidad.
4. Relaciona, cuando conozcas la equivalencia, procesos clásicos o importados desde CSV. La base no deduce la identidad por semejanza de empresa/puesto. La vía clásica/CSV sigue funcionando sin exigir esa revisión de partida.
5. Configura opcionalmente un tablón Greenhouse o tu destino ntfy según [integraciones](#configurar-integraciones-externas). Ambos comienzan desactivados.

Las automatizaciones comienzan desactivadas. Mi perfil permite consultar Greenhouse cada 15–1.440 minutos y generar avisos de tareas, entrevistas y nuevas oportunidades. Integraciones muestra el historial. Las consultas y entregas se revisan cada minuto con la aplicación encendida. La cola recupera PENDING al reiniciar; SENDING pasa a UNCERTAIN. Los avisos inciertos y rechazados no se reintentan automáticamente. Un resultado incierto puede corresponder a un mensaje recibido por el proveedor. No se promete entrega exactamente una vez.

## Calidad y pruebas

```powershell
.\scripts\quality.ps1
.\scripts\test.ps1
```

Con Bash: `bash scripts/quality.sh` y `bash scripts/test.sh`. Con Make: `make quality`, `make test`, `make help`. Para aplicar formato: `docker compose -f compose.quality.yaml run --rm --build format`.

Se mantienen Spotless con el perfil del proyecto, Checkstyle, PMD, duplicados por módulo y entre módulos, ArchUnit y JaCoCo. Las pruebas completas usan PostgreSQL temporal y Chromium con datos sintéticos y HTTP local. Fuera de Docker se necesita JDK 25: `./mvnw test` ejecuta unitarias/arquitectura; los scripts completos añaden integración y navegador.

## Decisiones de arquitectura

Consulta los [ADR](docs/adr/README.md), incluido ADR-0020 para automatización y entrega conservadora.

## Estilo de código compartido

El proyecto utiliza [Clean Code - Project Style 1.0](#perfil-completo-de-estilo): cuatro espacios, 120 columnas y llaves de métodos y constructores en la línea siguiente. Spotless y Checkstyle comprueban las mismas reglas desde Maven, el precommit y el check independiente `Code style` de las pull requests.

```sh
docker compose -f compose.style.yaml run --rm --no-deps check
docker compose -f compose.style.yaml run --rm --no-deps format
```

La guía documenta la instalación del precommit en cada clon y los límites de la comprobación automática.


## Arquitectura y contratos

Un ejecutable Java, una instancia de aplicación y PostgreSQL. Se conservan los ocho módulos del taller 2 y se añaden búsqueda y notificaciones. Las decisiones tácticas responden a reglas concretas; los formularios y lecturas no requieren otra jerarquía de abstracciones.

| Capacidad | Responsabilidad y frontera |
| --- | --- |
| applications | JobApplication, actividades, historial y regla de proceso activo; publica ApplicationsApi. |
| search | Opportunity, SearchProfile, SalaryRange, comparación explicable, consulta de proveedores y ejecuciones de importación. |
| notifications | Solicitudes, preferencias traducidas, cola y estados de entrega. Publica NotificationRequests y NotificationPreferences. |
| reminders | Política de tareas vencidas y entrevistas en próximas 24 h; publica ReminderNotifications. |
| imports / exports | Conservan CSV con previsualización, idempotencia y registro de exportadores CSV/ICS. |
| insights | Lecturas mediante ApplicationsApi y cálculo de estadísticas. |
| app | Seguridad, recursos y composición de dependencias; planificador de tres hilos. |

### Modelo y lenguaje

Una oportunidad identifica un puesto confirmado; cada candidatura es un proceso con canal, estado, actividades e historial propios. El perfil pertenece al único propietario y no cambia los datos históricos. Empresa contratante y agencia son papeles distintos. La identidad no se deduce por semejanza de texto.

El contexto de seguimiento abarca oportunidades, procesos y actividades. Módulos Maven y contextos delimitados no son equivalentes. Notificaciones utiliza un modelo menor de solicitudes y resultados; no recibe entidades del seguimiento.

La regla de un proceso activo por oportunidad combina una política de dominio con un índice único parcial de PostgreSQL. Archivar no libera la identidad. Las revisiones usan versión optimista y la reasignación conserva auditoría. Tareas y entrevistas siguen dentro de la candidatura porque comparten sus reglas; la sincronización selectiva reduce escrituras sin atribuir al ORM la frontera del agregado.

### Dirección de las dependencias

`search -> notifications.api -> reminders.api -> applications.api` es la dirección entre artefactos relevantes. Notificaciones no importa búsqueda. El adaptador ProfileNotificationPreferences pertenece a search e implementa el puerto que necesita notificaciones. La composición inyecta la implementación. Aplicación y dominio no importan Spring, HTTP, SQL ni plantillas; los adaptadores contienen esas dependencias.

ArchUnit comprueba esa dirección, accesos públicos, reconstrucción de agregados y ciclos; Maven construye el grafo de artefactos. Una clase public disponible en el classpath no queda oculta por el mero hecho de usar Maven.

### Automatización y transacciones

ImportFeed termina HTTP y la traducción antes de comenzar su transacción. Las oportunidades nuevas, el éxito de la ejecución y la solicitud de resumen se confirman juntos. NotificationRequests solo encola; el almacén participa en la transacción local compartida. Es un acoplamiento deliberado al monolito y a su base, documentado en ADR-0020.

La exclusión synchronized de ImportFeed serializa consultas manuales y programadas dentro de una instancia. Cada minuto se revisa el último intento finalizado y el intervalo del perfil. No se reproducen consultas omitidas durante una parada. Una ejecución abierta pasa a INTERRUPTED al arrancar. La próxima consulta espera el intervalo desde esa recuperación.

Cada minuto se generan recordatorios autorizados y se procesan hasta 25 pendientes. Antes de reclamar uno se comprueban consentimiento, categorías, caducidad, silencio y actividad vigente. PENDING -> SENDING se confirma sin transacción exterior; solo después se realiza HTTP. No se usan brokers ni reintentos ciegos. SENT significa aceptación de ntfy; REJECTED y UNCERTAIN son terminales. Al arrancar SENDING pasa a UNCERTAIN y PENDING conserva su posibilidad de entrega.

El planificador tiene tres hilos para evitar que una importación lenta ocupe el único hilo de recordatorios o entrega. No ofrece ejecución con la aplicación apagada, garantías de tiempo real ni soporte para varias réplicas.

### Lecturas

Oportunidades, procesos relacionables, historial de notificaciones e importaciones usan páginas de 25. Las oportunidades admiten empresa, puesto, modalidad y procedencia; los procesos se buscan por empresa o puesto. Los patrones SQL escapan los caracteres comodín introducidos por el usuario. Los filtros no se utilizan para escribir saltándose el modelo.

Consulta también [integraciones](#configurar-integraciones-externas), [migración](#migración-y-recuperación), [diseño](#diseño-del-backoffice) y [verificación](#comprobaciones-y-límites).


## Configurar integraciones externas

### Greenhouse

Contrato: GET `https://boards-api.greenhouse.io/v1/boards/{board}/jobs`, documentado por [Greenhouse](https://docs.greenhouse.io/job-board.html). Configurar en `.env`:

```dotenv
TRACKER_GREENHOUSE_BOARD=your-board-token
TRACKER_GREENHOUSE_EMPLOYER=Your hiring company
```

Recrear la aplicación con `docker compose up -d --build --wait`. El tablón solo admite letras ASCII, dígitos, guion y guion bajo, hasta 80 caracteres; la empresa es explícita. La UI no acepta una URL arbitraria de proveedor. Importar desde Oportunidades ejecuta la consulta manualmente. Mi perfil permite programarla, desactivada por defecto, con un intervalo de 15–1.440 minutos (inicial: 60). Integraciones muestra origen, fechas, estado y cantidades de cada ejecución.

Se utilizan id de publicación (no internal_job_id), título, location.name y absolute_url. La clave `greenhouse:board:id` evita colisiones entre tablones. Sin datos fiables, salario y modalidad permanecen sin confirmar. No se incorpora HTML remoto. No se envían candidaturas al proveedor.

Límites: conexión 3 s, petición 8 s, cuerpo recibido máximo 2 MiB antes de parsear, hasta 1.000 publicaciones, sin redirecciones. Una respuesta no 200, campos inválidos, exceso de límite o fallo de lectura impide importar. La petición y la traducción concluyen antes de iniciar la transacción local. Todo el lote nuevo, el resultado de la ejecución y su resumen pendiente autorizado se confirman juntos en la misma base; claves conocidas se omiten sin sobrescribir. Una carrera de clave única revierte el lote conflictivo; la siguiente consulta vuelve a determinar qué existe.

### ntfy

Contrato: POST de texto a un tema HTTPS administrado por el propietario, según [ntfy](https://docs.ntfy.sh/publish/).

```dotenv
TRACKER_NTFY_TOPIC=https://your-ntfy-server.example/your-private-topic
TRACKER_NTFY_TOKEN=your-private-access-token
```

El token es opcional según el servidor. Se envía en Authorization, nunca por URL, plantilla o registro de aplicación. No se admiten credenciales incrustadas, query ni fragmentos en el destino. Los secretos se guardan solo en `.env`, ya ignorado. No publicar el nombre de un tema privado. Configurar acceso de lectura/escritura adecuado en el servidor; un tema público no autentica al lector.

La configuración habilita la capacidad. Mi perfil requiere consentimiento y permite fijar zona, silencio y categorías. La generación automática empieza desactivada. También se pueden solicitar recordatorios vigentes desde Notificaciones; quedan pendientes para la revisión de la cola, como máximo 25 por ciclo de un minuto. La solicitud manual puede esperar al final del silencio y no requiere activar la generación automática.

Los recordatorios se obtienen de actividades actuales: tareas vencidas y entrevistas entre ahora y las próximas 24 horas. Cada importación con novedades puede producir un resumen; una repetida sin altas no produce otro. Los resúmenes caducan en 24 horas. Las automatizaciones requieren una instancia de aplicación encendida. El intervalo de importación cuenta desde el último intento finalizado, también si falla; no se recuperan ejecuciones omitidas.

Texto externo: `You have a pending reminder in Job Tracker.` o `Job Tracker found N new opportunities.` Sin empresa, título, notas, nombre de perfil o salario. Tiempo de conexión 3 s y petición 8 s, sin redirecciones.

La solicitud se guarda en PENDING, única por actividad/tipo/vencimiento o por ejecución de importación. Antes de enviar se confirma SENDING sin transacción exterior. SENT significa HTTP 2xx, no lectura; 4xx es REJECTED y 3xx, 5xx, corte o interrupción es UNCERTAIN. Al arrancar se recuperan pendientes y SENDING pasa a UNCERTAIN. No se repiten rechazados ni inciertos y no hay botón de reintento. Se prioriza evitar duplicados admitiendo posible pérdida.

El silencio aplaza. Revocar el consentimiento cancela todos los pendientes; desactivar automatización o categoría cancela los automáticos correspondientes. Cancelados y caducados conservan su clave y no se reactivan al habilitar preferencias. Un cambio posterior a iniciar la petición no puede revocarla. El canal no contacta un destino real durante las pruebas.

Las pruebas del adaptador pueden usar HTTP en bucle local a través de su constructor. La composición de producción exige HTTPS; no se añade una opción para desactivarlo desde la UI.


## Migración y recuperación

El origen local verificado es `823b95e`, árbol limpio, con suite completa satisfactoria antes de cambiarlo. Las versiones y dependencias técnicas se conservan; el reactor pasa a `3.0.0-SNAPSHOT` con artifactId `job-application-tracker-ddd`.

### Esquema

- V1–V3 se conservan sin modificación.
- V4 crea oportunidades y perfil. Conserva cada UUID histórico, fechas, notas, estados y colecciones. Una oportunidad provisional por proceso: `legacy:UUID`. No infiere agencias, salarios ni equivalencias por nombre. Crea la relación y el índice parcial de procesos activos.
- V5 añade el registro de intentos de entrega, vacío de partida.
- V6 añade la auditoría de revisiones de identidad.

El perfil comienza con nombre «Mi perfil», zona Europe/Madrid, consentimiento externo desactivado y silencio 22:00–08:00. La zona general de la interfaz sigue siendo TRACKER_ZONE y puede diferir de esta preferencia personal.

### Datos existentes

1. Detener escrituras en el taller 2 y obtener una copia con sus scripts de backup. Conservar su configuración privada fuera de Git.
2. Crear la configuración privada del taller 3 con sus scripts setup. Nombre Compose, volumen y puerto son diferentes; puerto predeterminado 8092.
3. Restaurar expresamente la copia en la base nueva. Los scripts restore heredados tienen como destino predeterminado una base de comprobación; no confundir esa prueba con sustituir la base de uso. Comprobar su destino antes de restaurar.
4. Arrancar el taller 3: Flyway aplica V4–V8 y Hibernate valida el esquema.
5. Comprobar recuentos de procesos, notas, actividades y fechas. Revisar identidades desde Oportunidades; no fusionar textos automáticamente.
6. Conservar la copia previa hasta verificar el resultado. No arrancar la versión anterior contra el esquema ampliado. Para revertir, detener la nueva versión y recuperar la copia previa en el entorno anterior separado.

Las pruebas MigrationIT y DddMigrationIT cubren arranque desde cero y actualización desde V1/V3/V6 con datos sintéticos. No se ha migrado ninguna base personal del usuario durante este trabajo.


### Ampliación de automatización: V6 → V8

V7 añade preferencias con ambas automatizaciones desactivadas, intervalo inicial 60 y categorías seleccionadas, y crea feed_runs. V8 crea notification_queue y copia los intentos de V5 conservando su clave de actividad, tipo y vencimiento. SENDING se convierte en UNCERTAIN. No se alteran V1–V6 ni se habilita el reenvío de solicitudes antiguas.

El ejecutable nuevo no debe convivir con el anterior sobre la misma base. Se mantiene una instancia y el volumen independiente. Las migraciones no borran datos de uso. Para volver a la versión anterior, detener la nueva y restaurar la copia previa en su entorno separado, siguiendo los pasos anteriores. No se garantiza que el ejecutable antiguo entienda un esquema ampliado.


## Formatos de intercambio

### CSV de ofertas

Formato propio del taller, no la exportación real de un portal. UTF-8 (se admite BOM), coma, cabecera y comillas CSV. Máximo 1 MiB y 1.000 registros. Las cabeceras pueden cambiar de orden. No se admiten nombres desconocidos ni duplicados; el archivo ilegible se rechaza completo.

| Campo | Regla |
|---|---|
| source | Obligatorio, hasta 160 caracteres. Se recorta y convierte a minúsculas con Locale.ROOT. |
| external_id | Obligatorio, hasta 180 caracteres. Se recorta conservando mayúsculas/minúsculas. |
| company | Obligatorio, hasta 160 caracteres. |
| role | Obligatorio, hasta 180 caracteres. |
| url | Opcional, hasta 2.000 caracteres; HTTP(S), host válido y sin credenciales. |
| location | Opcional, hasta 160 caracteres. |
| work_mode | Opcional: UNSPECIFIED, REMOTE, HYBRID u ONSITE; se normaliza a mayúsculas. Vacío = UNSPECIFIED. |
| technologies | Opcional, hasta 500 caracteres. |

Los campos se recortan. Las ofertas se crean en SAVED sin fecha de solicitud, contacto ni notas personales. La previsualización utiliza filtros sin efectos externos y caduca a los 15 minutos; el contenido se conserva en sesión, no en campos ocultos. Confirmar vuelve a validar el estado actual y guarda cada fila válida por separado.

Resultados técnicos: NEW, REJECTED, DUPLICATE, CONFLICT, IMPORTED, PENDING. El número corresponde al registro de datos CSV, no a la línea física: una celda puede contener saltos de línea.

Clave: `(source normalizada, external_id)`. Una huella SHA-256 de los campos normalizados originales permite distinguir repetición de cambio. No se compara con las ediciones personales vigentes ni se sobrescriben. Las claves repetidas dentro del mismo archivo usan el mismo criterio. Las filas pendientes tras un fallo se revisan mediante una nueva previsualización.

Un archivo sin registros es válido y no guarda nada. No se buscan duplicados por similitud de empresa o puesto ni se cruzan portales: eso exigiría una política distinta.

### Plantilla de prueba

[examples/offers-template.csv](../examples/offers-template.csv) contiene dos registros válidos y las ocho cabeceras admitidas. El README explica cómo añadir un tercero y comprobar nuevas filas, repeticiones y conflictos. El mismo fichero se descarga desde la pantalla **Importar ofertas** mediante `/imports/template`, con autenticación. Maven lo incluye como recurso desde `examples/`, sin mantener otra copia editable.

El ejemplo `examples/offers.csv` del taller incluye además una fila inválida intencionada; la plantilla de prueba permite empezar con todas las filas válidas.

### CSV de candidaturas

Plugin applications-csv: id, company, role, status, work_mode, applied_on, updated_at. Incluye todas las coincidencias de los filtros del listado y su orden estable, con fecha de solicitud ISO y fechas de actualización ISO en UTC. Es un formato de salida, no una copia de seguridad ni el CSV de entrada.

El escape de CSV conserva comas, comillas y saltos de línea. Los textos que podrían interpretarse como fórmulas se anteponen con apóstrofo. Esa transformación se documenta y no debe utilizarse para reconstruir una copia exacta de datos arbitrarios.

### ICS de entrevistas

Plugin interviews-ics: entrevistas SCHEDULED de candidaturas no archivadas. Las fechas del formulario son inclusivas en la zona configurada; internamente se consulta un intervalo de instantes `[desde, hasta)`. Sin fechas se utiliza hoy y el mes siguiente.

UID estable basado en el identificador de entrevista; DTSTAMP de generación y DTSTART en UTC. Texto escapado, CRLF y plegado a 75 octetos sin cortar caracteres UTF-8. El modelo no tiene hora de fin: no se añaden DTEND ni DURATION. Las canceladas y completadas se excluyen. Reimportar el archivo en un calendario depende de ese cliente: no se promete sincronización ni retirada de citas antiguas.


## Diseño del backoffice

Se conserva el sistema visual del primer taller: fuente del sistema, navegación lateral oscura, contenido sobre fondo neutro, superficies blancas y acciones azules. Los colores, foco, formularios y espaciado proceden de los tokens y componentes de su hoja compartida.

Las pantallas nuevas usan la misma plantilla y navegación. Las tablas de previsualización admiten desplazamiento dentro de su contenedor, sin provocar desbordamiento de toda la página. Los campos de fichero se ajustan al ancho disponible. Formularios con etiquetas, foco visible y mensajes de error anunciados permiten el acceso con teclado.

Las pruebas de navegador comprueban los recorridos principales en escritorio (1.440 px) y móvil (390 px), utilizando datos ficticios.

### Taller 3

Oportunidades, Mi perfil y Notificaciones reutilizan layout, navegación, paneles, botones, formularios en dos columnas y adaptación a una columna en móvil. No se incorpora la identidad del portfolio. Las advertencias de compatibilidad son texto explicativo, sin ocultar ofertas ni comunicar el resultado solo mediante color. Las acciones tienen etiquetas explícitas: registrar un envío de hoy, confirmar una identidad y solicitar una entrega externa son operaciones diferentes.

Las listas de oportunidades y procesos revisables tienen búsqueda y páginas de 25. Integraciones y Notificaciones presentan historial paginado; Mi perfil separa consentimiento, automatizaciones y categorías. Los límites y la interpretación de SENDING/UNCERTAIN son visibles. Las pruebas nuevas guardan capturas de las tres pantallas a 1440 y 390 px, además de conservar las del taller anterior.


## Perfil completo de estilo

El proyecto utiliza **Clean Code - Project Style 1.0**: una política de estilo propia, versionada y comprobable desde Maven. Recoge criterios de legibilidad inspirados en Clean Code y decisiones concretas del proyecto. El resultado de la validación acredita estas reglas; no certifica todos los principios de diseño del libro.

No depende de un IDE. `config/checkstyle-clean-code.xml` utiliza comprobaciones nativas de Checkstyle. `config/eclipse-formatter.xml` define el formato exacto que Spotless aplica y verifica. No se ha implementado un analizador de código propio. Spotless es la autoridad para la indentación exacta y el reparto de continuaciones y paréntesis; Checkstyle no duplica esa decisión con otro algoritmo de indentación. Así se evita que un comprobador rechace la distribución que exige el otro.

### Reglas del perfil

| Regla | Aplicación y comprobación |
| --- | --- |
| UTF-8, finales LF, salto final y ausencia de espacios al final | Spotless; Checkstyle comprueba también el salto final y los tabuladores. |
| Cuatro espacios por nivel y cuatro para las continuaciones | Perfil de formato, comprobado por Spotless. |
| Hasta 120 columnas, exceptuando `package` e imports | Formateador y `LineLength`. |
| Llave de apertura de métodos, constructores y constructores compactos en la línea siguiente | Formateador y `LeftCurly`, con `nl` para `METHOD_DEF`, `CTOR_DEF` y `COMPACT_CTOR_DEF`. |
| Llaves de tipos y estructuras de control al final de su cabecera | Formateador y otra instancia de `LeftCurly`, con `eol`. |
| Llaves obligatorias en las estructuras de control | `NeedBraces`. |
| Cadenas de llamadas que necesitan varias líneas, con una llamada por línea | Perfil de formato, comprobado por `spotless:check`. Las cadenas cortas pueden conservarse en una línea. |
| Argumentos y parámetros distribuidos cuando no caben; cierres de llamadas multilínea separados por niveles | Perfil de formato, comprobado por Spotless. |
| Una sentencia y una declaración de variable por línea | `OneStatementPerLine` y `MultipleVariableDeclarations`. |
| Espacios coherentes en operadores, comas, paréntesis y genéricos | Formateador, `WhitespaceAfter`, `WhitespaceAround`, `ParenPad`, `MethodParamPad` y `GenericWhitespace`. |
| Una línea en blanco entre miembros, sin separar mecánicamente cada campo | Formateador y `EmptyLineSeparator`, permitiendo campos consecutivos. |
| Imports explícitos y sin imports sin utilizar | Spotless, `AvoidStarImport` y `UnusedImports`. |
| Nombres con convenciones de Java y orden de modificadores | Reglas nativas de nombres y `ModifierOrder`. |

La separación en fases lógicas dentro de un método se revisa manualmente: las instrucciones relacionadas permanecen juntas y una línea en blanco puede separar preparación, ejecución y construcción del resultado. El formateador conserva esa separación. Una herramienta no puede deducir siempre dónde cambia la intención del código.

El perfil no obliga a añadir comentarios que repitan el código. La pertinencia de los nombres y la documentación se revisa al leer el cambio. Las comprobaciones de diseño y las pruebas del proyecto conservan su propio alcance y no se utilizan como criterios de formato en este perfil.

### Comprobar y corregir el estilo

Con Docker en ejecución, desde la raíz del proyecto, en PowerShell, Bash o cualquier terminal:

```sh
docker compose -f compose.style.yaml run --rm --no-deps check
```

Para aplicar el formato de manera explícita:

```sh
docker compose -f compose.style.yaml run --rm --no-deps format
```

`check` ejecuta `mvn validate`: Spotless comprueba el formato y Checkstyle comprueba las reglas. Un incumplimiento devuelve un código distinto de cero. `format` ejecuta `spotless:apply`; después se deben revisar las diferencias y volver a comprobar. Ninguno de los comandos crea commits ni prepara archivos en Git.

La imagen de Maven con Java 25 se fija por etiqueta y digest en `compose.style.yaml`. Las versiones de Spotless, Eclipse JDT y Checkstyle se fijan en el POM. Una caché Maven en un volumen de Docker conserva las dependencias; la primera ejecución puede necesitar descargarlas. No se arranca la aplicación ni PostgreSQL.

También se puede utilizar el Maven Wrapper con JDK 25:

```sh
./mvnw validate
./mvnw spotless:apply
```

En PowerShell utilizar `.\mvnw.cmd` en lugar de `./mvnw`.

### Instalar precommit

El hook usa pre-commit 4.7.0, fijado en `requirements-style.txt`. Python 3.12 o posterior se utiliza para esta herramienta de desarrollo; la aplicación sigue siendo Java. Su instalación es local a cada clon.

En PowerShell:

```powershell
python -m venv .venv-style
.\.venv-style\Scripts\python.exe -m pip install -r requirements-style.txt
.\.venv-style\Scripts\python.exe -m pre_commit install
.\.venv-style\Scripts\python.exe -m pre_commit run --all-files
```

En Linux y macOS:

```sh
python3 -m venv .venv-style
.venv-style/bin/python -m pip install -r requirements-style.txt
.venv-style/bin/python -m pre_commit install
.venv-style/bin/python -m pre_commit run --all-files
```

`.pre-commit-config.yaml` ejecuta el control completo del índice mediante `scripts/check_project.py --staged`. El hook se ejecuta para cualquier commit. La invocación de Maven comprueba todos los módulos; no se presenta como una comprobación limitada a los archivos cambiados. `pass_filenames: false` evita convertir rutas en argumentos de las herramientas.

El hook comprueba y rechaza; no aplica el formateador ni ejecuta `git add`. La exportación del índice excluye también los archivos nuevos todavía sin preparar. Pre-commit aparta temporalmente los cambios no preparados de los archivos seguidos y los restaura al terminar. Esto permite comprobar la versión preparada cuando solo se quiere incorporar una parte de un archivo.

La configuración versionada no instala el hook en otros equipos: cada desarrollador debe ejecutar el paso de instalación. Los hooks locales pueden omitirse, por lo que CI vuelve a comprobar las mismas reglas.

### Integración continua y alcance

El workflow `Verify` incluye el job independiente `Code style`, visible en las pull requests como `Verify / Code style`. Ejecuta `docker compose -f compose.style.yaml run --rm --no-deps check` sobre las fuentes y pruebas Java de todos los módulos. Se ejecuta en paralelo con `tests` y devuelve su propio resultado; un incumplimiento hace fallar este check.

El job `tests` instala las versiones fijadas en `requirements-style.txt` y ejecuta `python scripts/check_project.py --staged`, igual que el hook local. Ese control incluye Ruff, mypy, Spotless, Checkstyle, PMD, CPD, ArchUnit y las pruebas con PostgreSQL y Chromium. El job de estilo ofrece además un resultado separado para localizar los fallos de formato.

Modificar una regla exige sincronizar Checkstyle y el formateador, aplicar el formato y verificar ejemplos correctos e incorrectos. La migración de estilo se revisa separadamente de los cambios de comportamiento. Las reglas de diseño, las pruebas funcionales y los estándares de otros proyectos mantienen su alcance propio.

### Referencias

- [Checkstyle: configuración de reglas](https://checkstyle.org/config.html).
- [Checkstyle: posición de llaves](https://checkstyle.org/checks/blocks/leftcurly.html).
- [Checkstyle: separación de miembros](https://checkstyle.org/checks/whitespace/emptylineseparator.html).
- [Spotless para Maven y su integración con Eclipse JDT](https://github.com/diffplug/spotless/tree/main/plugin-maven#eclipse-jdt).
- [Pre-commit: instalación y funcionamiento](https://pre-commit.com/).


## Comprobaciones y límites

El control completo es `python scripts/check_project.py`: revisa el adaptador Python con Ruff y mypy, y ejecuta Spotless, Checkstyle, PMD, CPD, ArchUnit, pruebas unitarias, PostgreSQL temporal y Chromium. El precommit y CI ejecutan `python scripts/check_project.py --staged`, que exporta exactamente el índice a una carpeta temporal; los archivos sin preparar no se incluyen ni se modifican. No crea commits ni ejecuta git add.

Instala las herramientas y el hook en cada clon con los comandos anteriores de `.venv-style`, `pip install -r requirements-style.txt` y `python -m pre_commit install`. La configuración versionada no sustituye ese paso. El hook de calidad es `project-quality`; `pre_commit run --all-files` ejecuta ese control del índice. Para revisar cambios todavía sin preparar, utiliza el comando completo sin `--staged`.

El planificador ejecuta tres trabajos locales; solo se soporta una instancia de la aplicación. El silencio aplaza pendientes, la revocación los cancela y los resúmenes caducan en 24 horas. No se reintentan envíos rechazados ni inciertos. SENT indica aceptación del proveedor, no lectura. Las pruebas externas utilizan HTTP local y datos sintéticos.

Los informes generados quedan en `target/`; los del índice aislado se copian a `target/precommit/`. No se versionan informes internos ni materiales del artículo. La documentación de uso se concentra aquí y las decisiones en `docs/adr/`; el perfil `docs/code-style.md` se conserva como la referencia compartida expresamente acordada.

El estilo del adaptador Python sigue PEP 8/257: cuatro espacios, 79 columnas, imports explícitos, anotaciones de tipos y main protegido. Ruff 0.17.3 y mypy 2.4.1 se fijan junto a pre-commit 4.7.0 en requirements-style.txt. No introduce clases ni dependencias de ejecución en la aplicación Java. Las herramientas comprueban reglas concretas; la revisión manual decide nombres, responsabilidades y separación de fases.

## Plantilla CSV y restauración aislada

La plantilla de importación está en `examples/offers-template.csv`. Revisa su cabecera y conserva UTF-8; las reglas de columnas y límites están descritas en el apartado de intercambio de datos.

Para comprobar una copia sin sustituir los datos de trabajo, ejecuta `.\scripts\restore.ps1 -Path <copia.dump>`. El destino predeterminado es una base nueva llamada `tracker_restore`; debe no existir. Revisa esa base antes de decidir una sustitución. Restaurar sobre `tracker` exige indicar `-Database tracker -ReplaceExisting`, detiene la aplicación y sustituye esa base, por lo que requiere una copia verificada previa.
