# ADR-0018 · Avisos externos explícitos con incertidumbre conservada

Estado: Aceptada · 24/10/2026.

## Contexto

Un proveedor puede aceptar un mensaje y perderse su respuesta; una caída puede ocurrir entre registro y red.

## Decisión

Opt-in y silencio del perfil; acción manual; claim persistente antes de red; sin reintentos automáticos. El aviso solo comunica existencia de un pendiente.

## Alternativas

Enviar al guardar un agregado; reintentar cortes como si fueran rechazos; prometer entrega exactamente una vez.

## Consecuencias

Evita repeticiones automáticas, pero puede perder un aviso. SENDING tras caída y UNCERTAIN necesitan revisión; no hay reconciliación automática.

## Revisar si

Cuando el negocio requiera entrega recuperable, reintento con política explícita o avisos automáticos.


Actualización del taller: véase [ADR-0020](0020-automatizacion-y-cola-local.md). Se conserva esta decisión como registro histórico; 0020 la sustituye en el alcance indicado.
