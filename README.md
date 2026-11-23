# ACIS · Sistema de Gestión de Órdenes de Trabajo

Aplicación móvil Android nativa desarrollada en **Kotlin** con **Jetpack Compose (Material 3)** para la gestión en terreno de órdenes de trabajo (OT) de mantenimiento de **climatización y refrigeración comercial e industrial**.

---

##  Sobre la Empresa y el Proyecto

**ACIS Servicios Técnicos Ltda.** presta servicios de ingeniería, instalación, reparación y mantenimiento preventivo/correctivo para sistemas de climatización (HVAC - aire acondicionado, chillers, fancoils) y equipos de refrigeración comercial (cámaras frigoríficas, congeladores) en locales comerciales, supermercados e industrias.

Esta aplicación reemplaza los registros en papel y mensajería por una solución móvil que guarda las órdenes **localmente, sin necesidad de conexión a internet**, conservando el estado actual y los datos registrados del trabajo en terreno.

---

##  Funcionalidades Principales

La aplicación ofrece cuatro roles locales: administrador, planificador, técnico y cliente. Los tres últimos participan del ciclo de una Orden de Trabajo.

### Administración de cuentas

El administrador consulta las cuentas, cambia sus roles y las activa o desactiva. No administra órdenes y no puede desactivar su propia cuenta ni quitarse el rol administrador. Los cambios se guardan en SQLite y se aplican al siguiente inicio de sesión de la cuenta modificada. El registro de cuentas nuevas y la gestión de acceso mediante backend siguen pendientes.

### 1. Perfil Planificador
- **Creación de OTs**: Formulario para registrar cliente, local, equipo, descripción del trabajo, tipo (preventiva/correctiva), prioridad y fecha/hora programada. Después del primer intento de guardar, los errores se actualizan al editar los campos.
- **Asignación**: Asigna la orden de trabajo creada al técnico de demostración.
- **Botón Flotante (FAB)**: Acceso directo `+` con estilo Material 3 para crear nuevas órdenes.

### 2. Perfil Técnico
- **Aceptación de la OT**: El técnico confirma la recepción del encargo.
- **Check-in con ubicación aproximada**: Registro de llegada con captura puntual mediante `LocationManager.NETWORK_PROVIDER`. Si se deniega el permiso o no se obtiene ubicación, se registra la visita sin coordenadas.
- **Fotografía de Evidencia**: Captura de fotografías en terreno utilizando la cámara nativa del sistema (`FileProvider`).
- **Borrador de Visita**: Posibilidad de guardar avances parciales sin perder información.
- **Informe Técnico & Check-out**: Exige diagnóstico y actividades ejecutadas al finalizar la visita; los materiales son opcionales.

### 3. Perfil Cliente
- **Consulta de Estado**: Visualización de las órdenes pertenecientes a sus locales.
- **Conformidad u Observación**: Permite aprobar el trabajo realizado o registrar observaciones si requiere revisión.
- **Respuesta Técnica**: Espacio para que el técnico responda y aclare las observaciones formuladas.

---

##  Tecnologías y Arquitectura

- **Lenguaje**: Kotlin
- **UI**: Jetpack Compose con **Material Design 3**
- **Arquitectura**: MVVM (Model-View-ViewModel) con separación en paquetes `ui`, `domain` y `data`. Es una separación por responsabilidades; el dominio todavía depende de modelos y cuentas definidos en `data`.
- **Persistencia Local**: Base de datos **SQLite** (`SQLiteOpenHelper`) para soporte offline
- **Corutinas**: `viewModelScope`, `Dispatchers.IO` y `Mutex` para operaciones de persistencia sin bloquear la interfaz.
- **Acceso local**: usuarios en SQLite, contraseñas derivadas con sal y permisos por rol y propietario de la OT.
- **Recursos Nativos**:
  - `LocationManager` (Permisos de ubicación puntual)
  - `FileProvider` (Cámara nativa y evidencias fotográficas)
- **Navegación**: `androidx.navigation:navigation-compose`
- **Identidad de Diseño**: Paleta de colores M3 *"Frío Ártico & Cero Grados"* adaptada a la industria HVAC/Refrigeración.

---

##  Estructura del Código

```text
com.proyecto.acis/
├── data/
│   ├── OrdenTrabajo.kt          # Modelos de datos y enums (Perfil, Estado, Tipo, Prioridad)
│   ├── OrdenesDb.kt             # Gestor de base de datos SQLite local
│   ├── OrdenesRepository.kt     # Repositorio local con StateFlow
│   ├── Usuario.kt               # Identidad, cuentas demo y derivación de contraseñas
│   └── UbicacionCheckIn.kt      # Ubicación aproximada mediante LocationManager
├── domain/
│   ├── PermisosOt.kt             # Acceso según rol e identidad de la cuenta
│   ├── PermisosUsuarios.kt       # Administración de accesos y protección de la propia cuenta
│   └── ValidadorOt.kt           # Reglas de validación desacopladas
├── ui/
│   ├── theme/                   # Paleta M3 "Frío Ártico" (Color.kt, Theme.kt, Type.kt)
│   ├── OrdenesViewModel.kt      # ViewModel con manejo de estado y filtros por perfil
│   ├── LoginScreen.kt            # Formulario de inicio de sesión
│   ├── components/
│   │   ├── CamposOrden.kt       # Campos, selector de cliente, errores e indicadores compartidos
│   │   └── SuccessToast.kt      # Aviso visual reutilizable
│   ├── navigation/
│   │   ├── AcisApp.kt           # Entrada por rol, destinos y barra fija
│   │   └── RutasAcis.kt         # Nombres de rutas y construcción de destinos por ID
│   └── screens/
│       ├── ordenes/
│       │   ├── ListaOrdenes.kt  # Búsqueda, filtros y tarjetas
│       │   ├── CrearOrden.kt    # Formulario de creación
│       │   ├── EditarOrden.kt   # Formulario de edición
│       │   └── DetalleOrden.kt  # Visita, evidencia, informe y revisión
│       └── usuarios/
│           └── UsuariosScreen.kt # Lista de cuentas y diálogo de permisos
└── MainActivity.kt              # Punto de entrada de la aplicación
```

---

##  Requisitos para Ejecutar el Proyecto

1. **Android Studio** compatible con las versiones declaradas en el proyecto: Android Gradle Plugin `9.4.1` y Gradle Wrapper `9.6.0`. El daemon está configurado para JDK `25` en `gradle/gradle-daemon-jvm.properties`; la compatibilidad Java `11` del módulo es una configuración distinta.
2. **Android SDK 37** (mínimo soportado: Android 7.0 - API 24).
3. Clonar el repositorio y sincronizar Gradle:
   ```bash
   git clone https://github.com/AmaroBMZ/acis.git
   ```
4. Ejecutar el módulo `app` en un emulador o dispositivo físico.

## Rama de mejoras y cuentas de prueba

Rama: `mejoras/login-persistencia-pruebas`. Ejecutar **debug** instala `com.proyecto.acis.pruebas` junto a la aplicación original, con datos independientes.

| Rol | Correo | Contraseña |
| --- | --- | --- |
| Administrador | admin@acis.cl | Acis2026! |
| Planificador | planificador@acis.cl | Acis2026! |
| Técnico | tecnico@acis.cl | Acis2026! |
| Cliente | cliente@acis.cl | Acis2026! |
| Segundo cliente | cliente2@acis.cl | Acis2026! |

El segundo cliente comienza sin órdenes. Al crear o editar, el planificador selecciona el cliente propietario. Las órdenes de instalaciones anteriores quedan sin cliente hasta asignarlo explícitamente. Las siete órdenes de demostración se crean solo en la primera inicialización.

La lista permite buscar por OT/local/equipo y filtrar por estado. Para cambiar de cuenta, usar **Salir**. Es una demostración local sin backend ni sincronización entre dispositivos.

- [Criterios de aceptación y recorrido manual](docs/CRITERIOS_ACEPTACION.md)
- [Comparación con los ejemplos del profesor y decisión de base de datos](docs/REVISION_PROFESOR.md)
- [Comandos y resultados de validación](docs/VALIDACION.md)

---

## Guía para estudiar el código

Aunque los archivos están bajo `src/main/java`, el código de la aplicación está escrito en **Kotlin** (`.kt`). Los comentarios explican responsabilidades, estado y decisiones dentro de cada bloque.

### Revisión de comentarios del 23 de noviembre de 2026

Se revisaron los archivos Kotlin de la aplicación y las pruebas, además de la configuración Gradle y el manifiesto. Se completaron comentarios sobre los formularios de creación/edición, cámara, navegación por ID, validación de clientes activos, migraciones, publicación de cambios y manejo de errores. Esta revisión modifica comentarios y documentación; no agrega funcionalidades ni constituye una nueva ejecución de pruebas.

Los comentarios explican cada responsabilidad y los pasos que requieren razonamiento, sin repetir cada instrucción visual. Para seguir una acción, lee primero la pantalla, después su llamada al ViewModel y finalmente la regla de dominio y la operación del repositorio/base de datos.

| Punto de lectura | Qué conviene distinguir |
| --- | --- |
| `CrearOrden.kt` y `EditarOrden.kt` | Estado de los diálogos, borrador del ViewModel y datos persistidos son cosas distintas. Android cuenta meses desde cero. |
| `DetalleOrden.kt` | Capturar una foto y asociarla a una OT son dos resultados distintos; la cámara escribe el archivo y SQLite guarda su nombre. |
| `OrdenesViewModel.kt` | Un resultado `false` o `null`, una excepción y una llamada descartada por `ocupado` siguen caminos diferentes. |
| `OrdenesRepository.kt` y `OrdenesDb.kt` | Primero se guarda en SQLite y después se publica el estado en memoria; ambas operaciones no forman una única transacción. |
| `PersistenciaTest.kt` | Las bases temporales simulan versiones anteriores y se abren con el esquema actual v6. |

Orden recomendado de lectura:

1. `data/OrdenTrabajo.kt` y `data/Usuario.kt`: qué representa una orden, qué datos conserva y cuáles son los perfiles disponibles.
2. `domain/ValidadorOt.kt`, `domain/PermisosOt.kt` y `domain/PermisosUsuarios.kt`: validaciones y permisos separados para órdenes y administración de cuentas.
3. `data/OrdenesDb.kt`: cómo se crean las tablas y cómo se convierten filas SQL en objetos Kotlin.
4. `data/OrdenesRepository.kt`: cómo se guardan cambios, se controlan las transiciones de estado y se publica la lista actualizada.
5. `ui/OrdenesViewModel.kt`: cómo se conectan sesión, formularios, validaciones y operaciones del repositorio.
6. `MainActivity.kt`, `ui/LoginScreen.kt`, `ui/navigation/` y `ui/screens/ordenes/`: cómo arranca la aplicación, cómo se conectan los destinos y cómo las pantallas envían acciones.
7. `ui/theme/` y `ui/components/`: colores, tipografía y componentes visuales compartidos.
8. `ui/screens/usuarios/UsuariosScreen.kt`: cómo el administrador edita un borrador de permisos y espera la confirmación del guardado.

### Recorrido de una acción: modificar el acceso de una cuenta

1. El administrador selecciona **Modificar acceso**. La pantalla copia el rol y la activación al borrador del diálogo; todavía no modifica SQLite.
2. **Guardar** llama a `OrdenesViewModel.actualizarAcceso`, que comprueba la identidad del actor y la cuenta de destino mediante `PermisosUsuarios`.
3. `OrdenesRepository.actualizarAcceso` ejecuta la operación en `Dispatchers.IO`, protegida por `Mutex`.
4. `OrdenesDb.actualizarAcceso` relee las cuentas y vuelve a comprobar los permisos dentro de una transacción. Solo modifica rol y activación; conserva la contraseña y los datos de las órdenes.
5. Si se guarda, el repositorio recarga su `StateFlow` de usuarios y el callback devuelve `true`: se actualiza la lista y se cierra el diálogo. Si la operación devuelve `false`, el editor conserva el borrador y muestra el error. Si se lanza una excepción antes del callback, `ejecutar` la captura y publica `errorGeneral`, pero el diálogo administrativo no garantiza mostrarlo: su indicador `error` depende de ese callback.

`actor` es quien solicita el cambio; `destino` es la cuenta modificada. Un administrador activo puede cambiar otras cuentas, pero no desactivarse ni quitarse su propio rol. Cambiar el rol de una cuenta no reasigna sus órdenes. El técnico de demostración solo puede recibir nuevas asignaciones si sigue activo y tiene rol técnico.

### Navegación después de guardar

| Acción | Comportamiento implementado tras confirmar el guardado |
| --- | --- |
| Crear OT | Cierra el formulario y vuelve a la lista. |
| Editar OT | Vuelve al detalle anterior. |
| Finalizar visita, aprobar, observar o responder | Vuelve a la pantalla anterior tras persistir. |
| Modificar acceso de una cuenta | Cierra el diálogo y actualiza la tarjeta. |
| Guardar borrador, capturar foto o iniciar visita | Permanece en el detalle para continuar trabajando. |

La barra con **Volver** y **Salir** está fuera de la lista desplazable. En administración, **Salir** permanece fijo y **Cancelar** cierra el editor sin guardar. Durante una operación se bloquean las acciones de navegación correspondientes para evitar abandonar un guardado en curso.

### Recorrido de una acción: guardar una OT

```text
CrearOrden (pantalla)
  -> OrdenesViewModel.crear (comprueba rol y coordina la operación)
  -> ValidadorOt.crear (comprueba los campos)
  -> OrdenesRepository.crear (construye la OT y ejecuta en IO)
  -> OrdenesDb.insertar (guarda en SQLite)
  -> StateFlow publica la lista actualizada
  -> collectAsState permite que Compose actualice la pantalla
```

La pantalla recibe el ID mediante `onResultado` y vuelve a la lista solo si se creó la orden. Una validación fallida muestra errores; un fallo de persistencia no añade la orden a la lista en memoria.

La barra superior mantiene **Volver** y **Salir** fuera del contenido desplazable. Actualizar una OT vuelve al detalle; crear, finalizar una visita, aprobar, observar o responder regresa a la pantalla anterior tras confirmar el guardado. Guardar borrador, tomar una foto e iniciar la visita permiten continuar trabajando en el detalle.

Como analogía, la pantalla es el mostrador, el ViewModel coordina la solicitud, el validador revisa sus requisitos y el repositorio gestiona el archivo de órdenes. En el negocio, esta distribución permite cambiar la presentación sin escribir consultas SQL dentro de cada botón.

### Conceptos presentes en el proyecto

| Elemento | Uso concreto |
| --- | --- |
| `data class` y `val` | Representan datos de una OT; `copy(...)` crea una versión con cambios sin modificar la instancia anterior. |
| `enum class` | Limita perfiles, estados, tipos y prioridades a valores conocidos. `name` se guarda en SQLite y `etiqueta` se muestra en pantalla. |
| Encapsulamiento | `_ordenes` es privado y `asStateFlow()` permite observarlo sin modificarlo desde la UI. Los estados del ViewModel usan `private set`. |
| Herencia y `override` | `OrdenesDb` extiende `SQLiteOpenHelper` y define `onCreate`/`onUpgrade`; `MainActivity` extiende `ComponentActivity`. |
| `object` | Agrupa reglas o utilidades compartidas, como `ValidadorOt`, `PermisosOt` y `Claves`. |
| Funciones como parámetros | `cambiar` recibe una transformación de la OT; los callbacks de pantalla reciben resultados o comunican acciones. |
| `remember` y `rememberSaveable` | Conservan estado de interfaz entre recomposiciones; `rememberSaveable` también participa en la restauración de estado de Android. No reemplazan SQLite. |
| `suspend`, IO y `Mutex` | Las tareas de persistencia se ejecutan fuera del hilo principal y el repositorio serializa el acceso a sus datos. |

### Resultados, errores y estado temporal

`OrdenesViewModel.ejecutar` permite una acción a la vez. Si `ocupado` ya es `true`, descarta la nueva llamada sin ejecutar su callback. Si la acción lanza una excepción, publica un mensaje general y libera `ocupado` en `finally`; no convierte automáticamente esa excepción en un resultado `false`. Una cancelación de corutina se propaga.

En edición, `LaunchedEffect(orden.id)` carga la OT al entrar en composición o cambiar de ID. No sobrescribe el formulario en cada recomposición, pero una nueva entrada en composición vuelve a cargar los datos guardados. Los selectores de fecha/hora parten de la fecha actual, aunque el botón muestre la fecha almacenada de la OT, hasta que el usuario elige un valor.

`SuccessToast` es un componente Compose controlado por `show`, no un Toast del sistema ni una confirmación de persistencia. Al actualizar una OT se activa y se navega inmediatamente hacia atrás; su duración visible no está garantizada.

## Ciclo de vida y permisos de una orden

| Estado de origen | Acción | Rol | Estado resultante |
| --- | --- | --- | --- |
| `CREADA` | Asignar al técnico demo | Planificador | `ASIGNADA` |
| `ASIGNADA` | Aceptar encargo | Técnico asignado | `ACEPTADA` |
| `ACEPTADA` | Registrar check-in | Técnico asignado | `EN_VISITA` |
| `EN_VISITA` | Guardar borrador o foto | Técnico asignado | `EN_VISITA` |
| `EN_VISITA` | Finalizar con diagnóstico y actividad | Técnico asignado | `PENDIENTE_CONFORMIDAD` |
| `PENDIENTE_CONFORMIDAD` | Registrar observación | Cliente propietario | `OBSERVADA` |
| `OBSERVADA` | Responder observación | Técnico asignado | `PENDIENTE_CONFORMIDAD` |
| `PENDIENTE_CONFORMIDAD` | Aprobar | Cliente propietario | `CERRADA` |

El ViewModel comprueba identidad y rol mediante `PermisosOt`. El repositorio comprueba el estado requerido y SQLite condiciona la actualización al estado anterior. Son controles distintos: poder ver una orden no autoriza cualquier acción sobre ella.

El planificador puede crear, editar y eliminar órdenes. La implementación actual no restringe la edición o eliminación a un estado particular. La asignación usa el técnico de demostración fijo; no existe un catálogo editable de técnicos.

## Persistencia, formularios y recursos del dispositivo

- **SQLite versión 6:** `acis_ordenes.db` contiene `ordenes`, `usuarios` y `configuracion`. La migración desde v5 agrega activación de cuentas y el administrador demo, conservando órdenes y contraseñas anteriores. No se utiliza Room.
- **Inicialización:** si corresponde, se importan órdenes antiguas desde SharedPreferences o se insertan siete ejemplos. La marca `inicializada` evita recrearlos después de borrar todas las órdenes.
- **Contraseñas:** se almacena una sal aleatoria y un hash derivado con PBKDF2, 120 000 iteraciones y 256 bits. Las cuentas y la clave de demostración están publicadas en el proyecto; no hay registro de usuarios ni autenticación remota.
- **Sesión:** el usuario autenticado vive en el ViewModel. No se implementa un inicio de sesión persistente tras terminar el proceso. Salir limpia los borradores en memoria, pero conserva las órdenes guardadas.
- **Validación:** crear/editar exige local, equipo, descripción, tipo, prioridad, fecha real `dd/MM/yyyy`, hora `HH:mm` y una cuenta cliente activa. El selector y la validación usan las cuentas cargadas desde SQLite. No se exige que la fecha sea futura. Los errores de creación se activan después del intento de guardar y se recalculan al editar.
- **Informe:** diagnóstico y actividad son obligatorios al finalizar; materiales y fotografía son opcionales. Escribir en el formulario no equivale a persistirlo: hay que guardar el borrador o finalizar.
- **Fotografía:** se conserva una referencia por OT en SQLite y el archivo en `filesDir/evidencias`. `FileProvider` entrega a la cámara una URI para escribir la imagen. Al reemplazar una foto se intenta eliminar la anterior después de guardar la nueva referencia.
- **Ubicación:** se solicita permiso aproximado, se consulta el proveedor de red y se espera hasta 12 segundos. La falta de permiso o ubicación no bloquea el check-in.
- **Búsqueda:** filtra por identificador, local o equipo y permite combinar un estado. La lista se ordena por última actualización; la tarjeta destacada toma la primera coincidencia, aunque su texto diga “última orden creada”.

Los datos se gestionan localmente: no hay backend ni sincronización de órdenes entre dispositivos. La conservación del estado actual tampoco constituye un historial completo de auditoría: una actualización sustituye los valores de la fila.

## Pruebas y ejecución desde PowerShell

Desde la raíz, con el JDK y SDK configurados para Gradle:

```powershell
# Pruebas de reglas y contraseñas en la JVM.
.\gradlew.bat :app:testDebugUnitTest

# Compilación del APK debug y análisis estático Android.
.\gradlew.bat :app:assembleDebug :app:lintDebug

# Requiere emulador o dispositivo conectado.
.\gradlew.bat :app:connectedDebugAndroidTest
```

| Ubicación | Qué comprueba |
| --- | --- |
| `app/src/test/.../ValidadorOtTest.kt` | Campos obligatorios, fechas reales, horas, cliente e informe. |
| `app/src/test/.../PermisosOtTest.kt` | Acceso por identidad, ausencia de sesión y separación de roles. |
| `app/src/test/.../PermisosUsuariosTest.kt` | Privilegios administrativos, cuentas inactivas y protección del propio acceso. |
| `app/src/test/.../ClavesTest.kt` | Clave correcta/incorrecta y efecto de usar sales diferentes. |
| `app/src/androidTest/.../PersistenciaTest.kt` | SQLite, reapertura, migraciones v4/v5, activación de cuentas, ciclo de estados y borrado sin regenerar ejemplos. |
| `app/src/androidTest/.../LoginNavegacionTest.kt` | Login, administrador, cambio de cuenta, búsqueda, navegación fija y permanencia ante formularios inválidos. |
| `app/src/androidTest/.../ExampleInstrumentedTest.kt` | Identificador de la aplicación debug instalada. |

Las pruebas de persistencia utilizan bases temporales con nombres `test-<UUID>.db`. Los resultados históricos y las comprobaciones manuales pendientes están en [VALIDACION.md](docs/VALIDACION.md); no deben interpretarse como una nueva ejecución cada vez que se modifica la documentación.

### Resultado registrado el 21 de noviembre de 2026

- **23 pruebas unitarias JVM aprobadas**; compilación debug y análisis Android completados.
- **12 casos instrumentados verificados en Samsung SM-S921B (Android 16):** 11 aprobaron en la ejecución completa y el caso de desplazamiento aprobó al repetirlo después de corregir su selector. No se ejecutó nuevamente toda la suite después de esa corrección.
- Cámara, ubicación física y todos los cierres tras guardados exitosos conservan comprobaciones manuales pendientes.

## Pendientes para la evaluación final

Los cuatro roles implementados funcionan localmente. Todavía falta implementar el backend propio con microservicios y base de datos externa, conectar el CRUD remoto, completar registro/recuperación de contraseña/edición de perfil personal e integrar una API externa mediante Retrofit.

También falta medir y alcanzar el 80 % de cobertura de lógica: el número de pruebas aprobadas no demuestra ese porcentaje. La APK disponible es debug; la entrega final requiere APK release firmado y documentación de endpoints, ejecución de servicios y firma.

La revisión de comentarios también identifica pendientes de interfaz: mostrar el error administrativo cuando hay una excepción antes del callback, abrir los selectores de edición con la fecha/hora guardada y comprobar la visibilidad del aviso de éxito al navegar. Se documentan como comportamiento actual y puntos de mejora; no se corrigieron en esta revisión.

**Pregunta para estudiar:** si la pantalla ya oculta los botones según el perfil, ¿por qué el ViewModel vuelve a comprobar los permisos y el repositorio comprueba además el estado de la OT?

##  Información Académica

- **Asignatura**: Desarrollo de Aplicaciones Móviles (DSY1105)
- **Institución**: Duoc UC
- **Integrante**: Amaro Barria, Felipe Hernadez
