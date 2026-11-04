# ADR-0016 · Conservar actividades y aplicar escrituras selectivas

Estado: Aceptada · 24/10/2026.

## Contexto

Las colecciones se rehacían al guardar; no hay todavía reglas independientes para sus ciclos de vida.

## Decisión

Mantener el agregado de candidatura e historial atómico; sincronizar embeddables por valor y posición.

## Alternativas

Partir entrevistas/tareas por motivos exclusivos de rendimiento; conservar clear y reconstrucción.

## Consecuencias

Se evitan escrituras de posiciones sin cambios. Se conserva el coste de carga y los conflictos de versión compartidos. No se promete mejora temporal no medida.

## Revisar si

Cuando reservas de agenda, participantes o concurrencia requieran consistencia independiente.
