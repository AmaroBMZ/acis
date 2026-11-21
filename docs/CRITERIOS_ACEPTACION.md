# Criterios de aceptación de ACIS

Alcance: aplicación académica local, sin servidor ni sincronización entre dispositivos.
Propuesta de criterios para esta rama; no corresponden a una rúbrica entregada por el profesor.

| ID | Dado / Cuando | Resultado esperado | Verificación |
| --- | --- | --- | --- |
| CA-01 | Dada una cuenta de prueba, al ingresar correo y contraseña correctos | Accede con el rol registrado en SQLite; el correo admite espacios exteriores y mayúsculas | PersistenciaTest, LoginNavegacionTest |
| CA-02 | Al ingresar una contraseña incorrecta o un correo inexistente | Permanece en login, muestra error y permite reintentar | ClavesTest, PersistenciaTest, LoginNavegacionTest |
| CA-03 | Al cerrar sesión y entrar con otra cuenta | Descarta formularios y navegación anterior; no permite cambiar de rol desde la lista | LoginNavegacionTest; revisión de cerrarSesion |
| CA-04 | Al consultar órdenes con rol técnico o cliente | El técnico ve sus asignaciones; cada cliente ve solo las órdenes asociadas a su correo, en todos sus estados | PermisosOtTest |
| CA-05 | Al intentar una acción de otro rol o sobre una orden ajena | El ViewModel rechaza la operación antes de llamar al repositorio | PermisosOtTest; comprobaciones autorizado del ViewModel |
| CA-06 | Al crear o editar una OT como planificador | Exige local, equipo, descripción, cliente válido, tipo, prioridad, fecha real y hora válida; persiste los cambios | ValidadorOtTest, PersistenciaTest |
| CA-07 | Al recorrer el ciclo de trabajo | Solo permite creada → asignada → aceptada → en visita → pendiente de conformidad; observada → respuesta → pendiente; aprobación → cerrada | PersistenciaTest |
| CA-08 | Al finalizar visita con diagnóstico o actividad vacíos | No finaliza; muestra validación; materiales son opcionales | ValidadorOtTest, PersistenciaTest |
| CA-09 | Al cerrar y reabrir la base | Conserva órdenes, propietario, informe y cuentas; borrar todas las órdenes no vuelve a generar semillas | PersistenciaTest |
| CA-10 | Al actualizar una instalación con esquema v4 | Migra a v5 sin perder OTs; las antiguas quedan sin cliente hasta asignación explícita del planificador | PersistenciaTest |
| CA-11 | Al cargar, autenticar o guardar | SQLite y derivación de contraseñas se ejecutan en Dispatchers.IO; muestra progreso y serializa acciones para evitar dobles envíos | Revisión de enIo, Mutex y ejecutar; ejercicio manual |
| CA-12 | Al buscar y filtrar por estado | La lista muestra solo coincidencias de ID/local/equipo y estado; informa cantidad y ausencia de resultados | LoginNavegacionTest (búsqueda); ejercicio manual (filtros) |
| CA-13 | Al abrir crear, editar o detalle y volver | Mantiene una navegación coherente; crear limpia datos previos; una OT inexistente muestra salida de retorno | Revisión de rutas; ejercicio manual |
| CA-14 | Al denegar ubicación o no obtener señal | Permite check-in sin ubicación; no lanza error por falta de permiso | lint y ejercicio manual de permisos |
| CA-15 | Al eliminar una OT con evidencia | Borra primero el registro y luego el archivo; un fallo SQL no debe destruir previamente la fotografía | Revisión del repositorio; prueba manual con cámara |

## Recorrido manual de evaluación

1. Ejecutar la variante debug (`com.proyecto.acis.pruebas`). Entrar como planificador.
2. Crear una OT para `cliente2@acis.cl`; comprobar validaciones y asignarla al técnico.
3. Entrar como técnico, aceptar, iniciar visita (probar permiso concedido y denegado), guardar borrador, tomar foto y enviar informe.
4. Entrar como `cliente@acis.cl`: la OT creada para cliente2 no debe aparecer.
5. Entrar como cliente2, observar la OT. Volver al técnico y responder; volver al cliente2 y aprobar.
6. Buscar la OT por ID y probar el filtro Cerrada. Reiniciar la app, iniciar sesión y confirmar persistencia.
7. Como planificador, editar otra OT, cancelar y abrir Crear: no debe conservar los datos de edición.
8. Desde detalle, cerrar sesión y pulsar Atrás: no debe recuperar una pantalla autenticada.

No confundir una revisión de código con una prueba ejecutada: consultar VALIDACION.md para los resultados reales.
