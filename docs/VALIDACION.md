# Validación de la rama

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
