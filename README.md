# ACIS · Sistema de Gestión de Órdenes de Trabajo

Aplicación móvil Android nativa desarrollada en **Kotlin** con **Jetpack Compose (Material 3)** para la gestión en terreno de órdenes de trabajo (OT) de mantenimiento de **climatización y refrigeración comercial e industrial**.

---

## ❄️ Sobre la Empresa y el Proyecto

**ACIS Servicios Técnicos Ltda.** presta servicios de ingeniería, instalación, reparación y mantenimiento preventivo/correctivo para sistemas de climatización (HVAC - aire acondicionado, chillers, fancoils) y equipos de refrigeración comercial (cámaras frigoríficas, congeladores) en locales comerciales, supermercados e industrias.

Esta aplicación reemplaza los registros en papel y mensajería por una solución móvil que opera **sin necesidad de conexión a internet (Offline-First)**, manteniendo trazabilidad completa del trabajo realizado en terreno.

---

## 📱 Funcionalidades Principales

La aplicación permite recorrer el ciclo completo de una Orden de Trabajo utilizando un selector de perfiles de demostración:

### 1. Perfil Planificador
- **Creación de OTs**: Formulario con validaciones en tiempo real para registrar local, equipo, descripción del trabajo, tipo (preventiva/correctiva), prioridad y fecha/hora programada.
- **Asignación**: Asigna la orden de trabajo creada al técnico correspondiente.
- **Botón Flotante (FAB)**: Acceso directo `+` con estilo Material 3 para crear nuevas órdenes.

### 2. Perfil Técnico
- **Aceptación de la OT**: El técnico confirma la recepción del encargo.
- **Check-in con Ubicación GPS**: Registro de llegada con captura puntual de coordenadas geográficas (`LocationManager`).
- **Fotografía de Evidencia**: Captura de fotografías en terreno utilizando la cámara nativa del sistema (`FileProvider`).
- **Borrador de Visita**: Posibilidad de guardar avances parciales sin perder información.
- **Informe Técnico & Check-out**: Formulario obligatorio para registrar diagnóstico, actividades ejecutadas y materiales utilizados al finalizar la visita.

### 3. Perfil Cliente
- **Consulta de Estado**: Visualización de las órdenes pertenecientes a sus locales.
- **Conformidad u Observación**: Permite aprobar el trabajo realizado o registrar observaciones si requiere revisión.
- **Respuesta Técnica**: Espacio para que el técnico responda y aclare las observaciones formuladas.

---

## 🛠️ Tecnologías y Arquitectura

- **Lenguaje**: Kotlin
- **UI**: Jetpack Compose con **Material Design 3**
- **Arquitectura**: MVVM (Model-View-ViewModel) + Clean Architecture
- **Persistencia Local**: Base de datos **SQLite** (`SQLiteOpenHelper`) para soporte offline
- **Recursos Nativos**:
  - `LocationManager` (Permisos de ubicación puntual)
  - `FileProvider` (Cámara nativa y evidencias fotográficas)
- **Navegación**: `androidx.navigation:navigation-compose`
- **Identidad de Diseño**: Paleta de colores M3 *"Frío Ártico & Cero Grados"* adaptada a la industria HVAC/Refrigeración.

---

## 📁 Estructura del Código

```text
com.proyecto.acis/
├── data/
│   ├── OrdenTrabajo.kt          # Modelos de datos y enums (Perfil, Estado, Tipo, Prioridad)
│   ├── OrdenesDb.kt             # Gestor de base de datos SQLite local
│   ├── OrdenesRepository.kt     # Repositorio local con StateFlow
│   └── UbicacionCheckIn.kt      # Integración con GPS / LocationManager
├── domain/
│   └── ValidadorOt.kt           # Reglas de validación desacopladas
├── ui/
│   ├── theme/                   # Paleta M3 "Frío Ártico" (Color.kt, Theme.kt, Type.kt)
│   ├── OrdenesViewModel.kt      # ViewModel con manejo de estado y filtros por perfil
│   └── AcisApp.kt               # Interfaz de usuario Compose (Scaffold, TopAppBar, FAB, Cards)
└── MainActivity.kt              # Punto de entrada de la aplicación
```

---

## 🚀 Requisitos para Ejecutar el Proyecto

1. **Android Studio** Ladybug / Jellyfish o superior.
2. **Android SDK 37** (mínimo soportado: Android 7.0 - API 24).
3. Clonar el repositorio y sincronizar Gradle:
   ```bash
   git clone https://github.com/AmaroBMZ/acis.git
   ```
4. Ejecutar el módulo `app` en un emulador o dispositivo físico.

---

## 🎓 Información Académica

- **Asignatura**: Desarrollo de Aplicaciones Móviles (DSY1105)
- **Institución**: Duoc UC
- **Integrante**: Amaro Barria
