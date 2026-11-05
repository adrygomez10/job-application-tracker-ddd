# ADR-0021 · Subgrupos internos por responsabilidad

Estado: aceptada.

## Contexto

Los componentes ya tienen dominio, casos de uso y adaptadores, pero sus carpetas acumulan tipos con trabajos distintos. La raíz de candidaturas mezcla datos del proceso, actividades, historial, una política y validación de campos. Búsqueda reúne oportunidad y perfil, y las notificaciones mezclan generación, entrega y contratos de salida.

## Decisión

Crear subpaquetes por responsabilidad dentro de las capas existentes. El README enumera la distribución. No se añaden módulos Maven ni capas nuevas, ni se divide el agregado de candidatura. Los nombres describen datos o trabajo: candidatura, actividades, historial, estados, políticas, validación, importación, perfil, entrega y puertos.

`Checks` y `Text` conservan su papel de validación. `ApplicationDetails` permanece como objeto de valor. Los valores salariales y de modalidad compartidos por oportunidad y perfil permanecen en la raíz del dominio de búsqueda; el dominio de notificaciones conserva dos tipos sin carpetas artificiales.

Java no comparte visibilidad de paquete con sus subpaquetes. Se hacen accesibles las operaciones que necesitan cruzar esas agrupaciones y se restringe su uso con ArchUnit: solo la raíz modifica actividades y genera entradas nuevas de historial; los validadores permanecen dentro del dominio y el mapeo SQL dentro del adaptador de persistencia. Las pruebas incluyen consumidores deliberadamente inválidos para comprobar el rechazo. Se conservan las restricciones existentes de reconstrucción de snapshots y de uso de APIs públicas.

Las filas JPA y sus colaboradores permanecen juntas dentro del grupo de persistencia de candidaturas, conservando sus accesos internos. Los formularios de seguimiento también se mantienen con los controladores que utilizan sus métodos de paquete. Se conservan las cuatro fronteras previas entre casos de uso independientes y se añaden restricciones de ciclos para dominio, aplicación y adaptadores agrupados.

## Alternativas

- Conservar todas las clases al mismo nivel: navegación más sencilla inicialmente, pero mezcla responsabilidades al crecer.
- Clasificar todo como entidades, objetos de valor y servicios: comunica la categoría DDD, pero separa colaboradores que cambian juntos. Se utilizan responsabilidades del proyecto y grupos pequeños con una función reconocible.
- Hacer públicas todas las filas y operaciones para mover cada clase por separado: pierde encapsulación sin resolver una necesidad del caso de uso.
- Crear otros agregados o módulos por cada carpeta: confunde organización interna con consistencia y despliegue.

## Consecuencias

Se actualizan packages, imports, referencias de plantillas, configuración, pruebas y herramientas del artículo. Las migraciones y los contratos públicos entre componentes permanecen intactos. No hay cambio de comportamiento previsto.

La restricción de determinadas operaciones públicas pasa a depender de las comprobaciones de arquitectura, como ya ocurría con la reconstrucción de entidades. Una biblioteca publicada para consumidores que no ejecuten esos controles exigiría revisar esa decisión. Se evitarán subdivisiones que solo repitan nombres o fuercen a exponer nuevos detalles sin mejorar la comprensión.
