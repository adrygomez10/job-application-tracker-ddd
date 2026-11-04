# ADR-0015 · Una transacción local y un índice parcial para el proceso activo

Estado: Aceptada · 24/10/2026.

## Contexto

Consultar y guardar por separado permite carreras sobre una identidad conocida.

## Decisión

ActiveProcessPolicy expresa la regla. OpportunityProcesses usa UnitOfWork; PostgreSQL protege opportunity_id para tres estados activos, sin excluir archivados.

## Alternativas

Solo consulta previa; bloqueo global; coordinación distribuida; separar un servicio de candidaturas.

## Consecuencias

Una base arbitra la carrera y revierte el alta incompatible. La garantía depende de una identidad relacionada y del esquema persistente.

## Revisar si

Si hay varias convocatorias simultáneas de la misma oportunidad o se separa la autoridad de escritura.
