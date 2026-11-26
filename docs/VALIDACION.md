# Validación de la rama

## Separación inicial de sesión (26 de noviembre de 2026)

- Se separó `SesionViewModel` del ViewModel de órdenes, con `AutenticacionRepository` recibido por constructor y una identidad compartida en `SesionActual`.
- SQLite conserva el esquema v6, las cuentas, claves y órdenes. La autenticación utiliza un adaptador al repositorio existente; usuarios y órdenes aún no están separados completamente.
- **28 pruebas JVM aprobadas**, sin fallos ni errores: 23 existentes y 5 de sesión con una fuente simulada. Cubren carga fallida, reintento, credenciales rechazadas, cuenta inactiva, excepción, identidad compartida, llamadas duplicadas y logout.
- `assembleDebug` y `assembleDebugAndroidTest`: correctos. La compilación de pruebas Android se repitió después de agregar limpieza de sesión entre casos.
- `lintDebug`: 0 errores, 18 advertencias y 10 sugerencias. `git diff --check`: sin errores.
- No se ejecutaron pruebas instrumentadas: ADB no detectó dispositivos. El nuevo caso `salirLimpiaBorradoresYConservaOrdenesGuardadas` está compilado, pero su recorrido en UI sigue pendiente, al igual que cámara y ubicación física.
- La cobertura porcentual no fue medida en esta etapa; 28 casos aprobados no acreditan el 80 %.

Comandos ejecutados: `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, `:app:lintDebug`.

## Verificación en teléfono Samsung (21 de noviembre de 2026)

La variante `com.proyecto.acis.pruebas` se actualizó con `adb install -r` en el Samsung SM-S921B con Android 16. Se ejecutaron los 12 casos instrumentados: inicialmente aprobaron 11 y falló el selector de desplazamiento del caso nuevo de navegación.

Se corrigió la prueba para desplazar la LazyColumn por índice, distinguirla de los selectores horizontales y volver a localizar Guardar OT después de que los errores agregaran filas. La repetición individual de ese caso aprobó. Por tanto, los 12 casos tienen resultado satisfactorio entre la ejecución completa y la repetición dirigida; no se hizo otra ejecución completa posterior.

Quedan comprobados por esas pruebas: login/logout, entrada administrativa, permisos de acceso, Volver y Salir visibles al final del formulario, permanencia ante validación fallida, persistencia y migraciones v4/v5. Cámara, ubicación física y todos los cierres tras operaciones exitosas todavía requieren su recorrido manual. Las pruebas SQL utilizaron bases temporales independientes.

## Cuarto rol local (21 de noviembre de 2026)

- Administrador: lista cuentas y modifica rol/activación desde una pantalla propia. No accede a las órdenes ni puede quitarse su propio acceso administrativo.
- SQLite v6: migración desde v5 para agregar activación y administrador demo; las instalaciones anteriores a v5 crean directamente la tabla de usuarios actualizada.
- 23 pruebas JVM aprobadas: las 19 anteriores y 4 de permisos administrativos.
- APK debug y pruebas Android compilados. Se añadieron casos Android de migración v5, conservación de credenciales, activación y entrada a la pantalla administrativa.
- Las pruebas instrumentadas y la revisión visual siguen pendientes: no hay dispositivo ni emulador conectado. Compilar estos casos no equivale a ejecutarlos.
- El alcance sigue siendo local. Registro, recuperación, edición de perfil personal, microservicios, API externa y cobertura medida del 80 % siguen pendientes.

## Reorganización y navegación (20 de noviembre de 2026)

Se separaron navegación, pantallas de órdenes y componentes compartidos. La barra superior conserva Volver y Salir al desplazar el contenido. Las confirmaciones de creación, edición, informe y revisión navegan solo después de un guardado correcto.

- Compilación debug y 19 pruebas JVM: correctas.
- Compilación de pruebas Android: correcta. Se añadió un caso de formulario inválido y navegación visible después de desplazarlo.
- Android lint: ejecutado correctamente; consultar su reporte para advertencias.
- No se ejecutaron las pruebas Android de esta etapa: ADB no detectó dispositivos ni emuladores conectados. Sigue pendiente comprobar visualmente los recorridos y las confirmaciones de éxito/error.

En esta etapa de reorganización todavía no se había implementado el cuarto rol; su avance posterior se detalla arriba. El backend, registro, recuperación de contraseña y medición del 80 % de cobertura siguen pendientes.

Comandos desde la raíz, con el JDK de Android Studio:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

El segundo comando necesita un emulador o dispositivo conectado. Las pruebas de persistencia crean y eliminan únicamente bases `test-<UUID>.db`. La variante debug tiene identificador propio.

## Resultados (18 de noviembre de 2026)

| Verificación | Resultado |
| --- | --- |
| Compilación debug y generación de APK | Correcta |
| Pruebas unitarias JVM | 19 aprobadas: 11 validaciones, 6 permisos y 2 contraseñas |
| Pruebas en emulador Medium Phone API 37 | 8 aprobadas: 5 persistencia, 2 login/navegación y 1 identidad del paquete |
| Android lint | Cero errores; advertencias no bloqueantes sobre versiones, recursos y recomendaciones de código |
| Git diff --check | Sin errores de formato |

Las pruebas de persistencia cubren login correcto/incorrecto, consulta parametrizada, cambios de estado válidos e inválidos, creación/actualización, reapertura, borrado total sin regenerar semillas y migración v4 → v5 preservando registros. Las de interfaz verifican rechazo de credenciales, reintento, búsqueda sin coincidencias, logout y acceso de los tres roles.

Se corrigieron dos bloqueos encontrados durante la ejecución: comprobación explícita del permiso de ubicación y actualización de Espresso/AndroidX Test por incompatibilidad con el emulador. Un primer intento también encontró el emulador offline; se reconectó antes de ejecutar la suite.

## Pendiente de comprobación manual

- Cámara y fotografía real, GPS con permiso concedido/denegado y ausencia de señal.
- Recorrido completo de formularios, filtros de estado y botón Atrás desde cada pantalla, rotación y diferentes tamaños de dispositivo.
- Funcionamiento en Android API 24 (la ejecución instrumentada se realizó en API 37).

Estas comprobaciones no se presentan como aprobadas automáticamente. Seguir el recorrido de CRITERIOS_ACEPTACION.md.

Reportes generados por Gradle (no se versionan):

- `app/build/reports/tests/testDebugUnitTest/index.html`
- `app/build/reports/androidTests/connected/debug/index.html`
- `app/build/reports/lint-results-debug.html`
- APK: `app/build/outputs/apk/debug/app-debug.apk`
