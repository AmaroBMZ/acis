# Comparación del material y decisiones

## Material inspeccionado

- `Unitkot-master.zip`: `PersonViewModel` mantiene personas en memoria (`mutableStateOf(listOf<Persona>())`). No implementa persistencia. Sus tests usan JUnit 4 y la organización Given/When/Then; varios métodos contienen solamente TODO y `RegisterTest` está vacío. ACIS incorpora aserciones ejecutables de comportamiento, sin copiar esos tests vacíos.
- `EjercicioLayoutsConNav.zip`: `MainActivity` construye un NavHost con home/agregar. Las pantallas muestran layouts de Compose. Sus fuentes y dependencias no incluyen Room, SQLiteOpenHelper ni un DAO: este ZIP no permite atribuir al profesor una integración de base de datos.
- Carpeta preexistente `.revision-EjemploEv1`: `AppDatabase` usa `Room.databaseBuilder`, una entidad Persona, `PersonaDao` y métodos suspend; `getAll` devuelve Flow. Es el ejemplo disponible que sí muestra una base de datos local. No se modificó esta carpeta.

## Decisión para ACIS

ACIS ya guardaba órdenes mediante SQLiteOpenHelper, esquema v4. Se conserva esa implementación para migrar los registros existentes sin sustituir la base. **Esta rama no convierte SQLiteOpenHelper a Room.** Room también trabaja sobre SQLite, pero usa entidades, DAO y código generado en lugar del mapeo manual existente.

Se añade el esquema v5 con tabla usuarios, sal/hash por contraseña, clienteCorreo en órdenes y una marca transaccional de inicialización. No se usa una migración destructiva. Las OTs antiguas no tenían propietario: se conservan con correo vacío y el planificador debe seleccionarlo al editar. Las siete semillas nuevas pertenecen a cliente@acis.cl; cliente2 comienza sin órdenes, para comprobar separación de cuentas.

El repositorio ofrece operaciones suspend con `withContext(Dispatchers.IO)` y Mutex. El ViewModel llama mediante viewModelScope, mantiene progreso y errores, y comprueba permisos antes de modificar. Es una separación UI → ViewModel → repositorio → SQLite, con validadores y permisos independientes de Android.

Las pruebas de interfaz detectaron una incompatibilidad del Espresso 3.5.1 original con el emulador API 37 (`InputManager.getInstance`). Se actualizan Espresso a 3.7.0 y AndroidX JUnit a 1.3.0, versiones presentes también en Unitkot. La corrección de esa llamada reflectiva está documentada en las [notas oficiales de AndroidX Test](https://developer.android.com/jetpack/androidx/releases/test#espresso-3.7.0).

## Límites del login

Es un login académico **local** con cuentas demo conocidas; no autentica usuarios contra una API, no sincroniza equipos y no incluye registro ni recuperación de contraseña. Las contraseñas se guardan derivadas con PBKDF2 y sal aleatoria; no se almacenan en texto plano en SQLite. Las credenciales demo se documentan y están visibles en login para facilitar la evaluación.

La sesión se conserva durante recreaciones de la actividad mediante ViewModel, pero no después de que Android destruya el proceso. Cerrar sesión limpia los formularios y descarta el NavHost autenticado.

## Protección del original

El trabajo se realiza en `mejoras/login-persistencia-pruebas`. La variante debug usa `applicationIdSuffix = ".pruebas"`: se instala junto a la app original y emplea su propio almacenamiento. Por ello los datos originales no aparecerán automáticamente en la app de pruebas. La migración v4 → v5 se comprueba sobre una base temporal independiente.
