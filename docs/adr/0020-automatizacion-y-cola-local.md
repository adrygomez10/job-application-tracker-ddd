# ADR-0020 · Automatización configurable y cola local conservadora

Estado: aceptada. Sustituye ADR-0018 para la solicitud y entrega de avisos, y la parte de ADR-0019 relativa a la dirección del contrato de preferencias.

## Contexto

La consulta manual y un único intento de aviso permiten enseñar las fronteras iniciales, pero el propietario necesita consultar periódicamente un tablón, recibir recordatorios y conocer sus novedades. Puede apagar la aplicación o cambiar sus preferencias mientras hay trabajo pendiente. Un envío remoto puede recibirse aunque se pierda la respuesta.

## Decisión

Mantener una instancia del monolito con una base. El perfil activa por separado la consulta del tablón y los avisos automáticos, inicialmente desactivados. El intervalo es 15–1.440 minutos, con valor inicial 60. Un trabajo cada minuto decide si corresponde consultar; guarda cada ejecución y no acumula las omitidas. Entradas manuales y programadas pasan por la misma exclusión local.

Notificaciones posee NotificationRequests y el puerto NotificationPreferences. Un adaptador de búsqueda traduce el perfil, evitando el ciclo entre módulos. La importación confirma oportunidades, resultado y resumen pendiente en una transacción de PostgreSQL. Esta cola funciona como una outbox local de solicitudes de entrega, sin un bus genérico de eventos ni un broker.

Cada aviso tiene una clave única, común para entradas manuales y automáticas. El procesador comprueba preferencias y vigencia y confirma SENDING antes de HTTP. El adaptador rechaza una reserva dentro de una transacción exterior. Al reiniciar solo se recuperan pendientes no iniciados; los intentos interrumpidos pasan a inciertos. No se reintentan rechazados ni inciertos.

El silencio aplaza, la revocación cancela y los resúmenes caducan en 24 h. Las cancelaciones conservan la clave: volver a habilitar preferencias no reactiva automáticamente solicitudes anteriores. Una preferencia cambiada después de iniciar HTTP no revoca esa petición. Las notificaciones externas contienen solo un texto genérico o un recuento.

## Alternativas

- Mantener acciones manuales: menor complejidad, no satisface el seguimiento automático solicitado.
- Reintentar cualquier error: recupera algunas entregas, pero puede duplicar las recibidas cuya respuesta se perdió. Se elige evitar duplicados admitiendo posible pérdida.
- Broker y procesos separados: añaden infraestructura y coordinación distribuida sin una necesidad actual de despliegue independiente.
- Dependencia mutua entre búsqueda y notificaciones: genera un ciclo que se evita haciendo que el consumidor sea propietario del puerto de preferencias.

## Consecuencias y revisión

La cola, sus estados y la transacción compartida añaden código que debe probarse sobre PostgreSQL. No se ofrece exactamente una vez ni compatibilidad con varias réplicas. Se revisará ante exigencia de reenvío, reconciliación, despliegue independiente o varios propietarios. Las migraciones conservan los intentos existentes para no habilitar envíos duplicados al actualizar.
