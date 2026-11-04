# ADR-0019 · Un contexto de seguimiento, otro de entrega y perfil local

Estado: Aceptada · 24/10/2026.

## Contexto

Los módulos anteriores organizan capacidades; no todos representan un lenguaje de negocio independiente.

## Decisión

Conservar oportunidades, procesos y agenda en el contexto de seguimiento. Notificaciones recibe DTO mínimos. Un perfil por propietario con repositorio propio.

## Alternativas

Un contexto o microservicio por módulo Maven; modelo global compartido con proveedores; introducir multiusuario al añadir preferencias.

## Consecuencias

Las fronteras corresponden a modelos y contratos reales. Las consultas pueden reunir datos sin autorizar escrituras cruzadas.

## Revisar si

Cuando usuarios, autorizaciones, equipos o lenguaje divergente exijan revisar esas fronteras.


Actualización del taller: véase [ADR-0020](0020-automatizacion-y-cola-local.md). Se conserva esta decisión como registro histórico; 0020 la sustituye en el alcance indicado.
