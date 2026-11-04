# ADR-0017 · Proveedores detrás de puertos y traducción explícita

Estado: Aceptada · 24/10/2026.

## Contexto

Las publicaciones del tablón y las notificaciones tienen vocabulario y fallos externos.

## Decisión

OfferFeed y NotificationChannel con adaptadores Greenhouse/ntfy. Composición explícita, límites de HTTP, sin redirecciones, secretos fuera de dominio y UI.

## Alternativas

HTTP dentro de entidades; URLs arbitrarias del formulario; SDK del proveedor en casos de uso.

## Consecuencias

Se pueden probar fallos con servidores locales y cambiar adaptadores. Cada nuevo proveedor requiere validar su contrato real.

## Revisar si

Si cambia el contrato de un proveedor o es necesario conciliar cambios y entregas.
