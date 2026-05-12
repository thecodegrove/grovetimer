# GroveTimer - Android Application Development Guide

Este documento establece el procedimiento operativo estándar para cualquier asistente de desarrollo trabajando en la aplicación Android GroveTimer. Todas las acciones deben seguir este flujo de trabajo.

## 📱 Información General

**GroveTimer** es una aplicación Android nativa desarrollada con Kotlin y Jetpack Compose. La aplicación permite a los usuarios establecer temporizadores para pausar automáticamente el contenido multimedia que están reproduciendo.

**Paquete de la aplicación**: `com.thecodegrove.grovetimer`

## 🏗️ Arquitectura del Proyecto

### Arquitectura General

La aplicación sigue una arquitectura **Clean Architecture** con separación de capas:

```
com.thecodegrove.grovetimer/
├── data/              # Capa de datos
│   └── repository/    # Implementaciones de repositorios
├── domain/            # Capa de dominio (lógica de negocio)
│   ├── model/         # Modelos de dominio
│   ├── repository/    # Interfaces de repositorios
│   └── usecase/       # Casos de uso
├── ui/                # Capa de presentación
│   ├── common/        # Componentes UI reutilizables
│   ├── components/    # Componentes Compose específicos
│   ├── navigation/    # Navegación
│   ├── screens/       # Pantallas principales
│   ├── settings/      # Lógica de configuración
│   ├── theme/         # Tema y estilos
│   └── timer/          # Componentes relacionados con el temporizador
├── services/          # Servicios en segundo plano
│   ├── SleepTimerService.kt          # Servicio principal del temporizador
│   ├── TimerNotificationService.kt   # Servicio de notificaciones
│   └── MyNotificationListener.kt    # Listener de notificaciones
└── utils/             # Utilidades
    └── PermissionUtils.kt
```

### Patrón MVVM

La aplicación utiliza el patrón **Model-View-ViewModel**:

- **Model**: Capa de dominio (`domain/`) y datos (`data/`)
- **View**: Pantallas Compose (`ui/screens/`)
- **ViewModel**: `TimerViewModel`, `SettingsViewModel`

### Servicios en Segundo Plano

- **SleepTimerService**: Servicio foreground que gestiona el temporizador y controla la reproducción multimedia
- **TimerNotificationService**: Gestiona las notificaciones persistentes del temporizador
- **MyNotificationListener**: Servicio necesario para acceder a MediaSessionManager

## 🛠️ Stack Tecnológico

### Core
- **Kotlin**: Lenguaje principal
- **Android SDK**: Min SDK 28, Target SDK 36, Compile SDK 36
- **Java Version**: 11

### UI Framework
- **Jetpack Compose**: Framework de UI moderno
- **Material 3**: Sistema de diseño Material Design 3
- **Material Icons Extended**: Iconografía extendida

### Arquitectura y Estado
- **ViewModel**: Gestión del estado de la UI
- **StateFlow**: Flujos de estado reactivos
- **Coroutines**: Programación asíncrona

### Navegación
- **Navigation Compose**: Navegación entre pantallas

### Inyección de Dependencias
- **Hilt**: Framework de inyección de dependencias (configurado pero no completamente implementado)

### Persistencia
- **SharedPreferences**: Almacenamiento de configuraciones del usuario

## 📦 Estructura de Paquetes

El paquete base es `com.thecodegrove.grovetimer`. Todos los paquetes deben seguir esta convención:

```
com.thecodegrove.grovetimer
├── data.repository          # Implementaciones de repositorios
├── domain.model             # Modelos de dominio
├── domain.repository        # Interfaces de repositorios
├── domain.usecase          # Casos de uso
├── ui.common               # Componentes UI comunes
├── ui.components           # Componentes Compose
├── ui.navigation           # Navegación
├── ui.screens              # Pantallas
├── ui.settings             # ViewModels de configuración
├── ui.theme                # Tema y estilos
├── ui.timer                # Componentes de temporizador
├── services                # Servicios en segundo plano
└── utils                   # Utilidades
```

## 🎨 Guía de Estilo y Diseño

La aplicación sigue el **Style Guide de The Code Grove** ubicado en `design/code_grove_style_guide.html`.

### Principios de Diseño

1. **Minimalismo Intencional**: Cada elemento debe tener un propósito claro
2. **Crecimiento Orgánico**: Interfaces naturales e intuitivas
3. **Claridad Sobre Complejidad**: Priorizar facilidad de uso
4. **Consistencia Visual**: Mismos colores, tipografías y patrones

### Paleta de Colores

- **Leaf Light** (`#E8F5E8`): Fondos, superficies secundarias
- **Sprout** (`#C8E6C9`): Elementos secundarios, estados deshabilitados
- **Grove Green** (`#4CAF50`): Color primario, acciones principales, acentos
- **Root Brown** (`#5D4037`): Texto principal, elementos estructurales

Los colores están definidos en `ui/theme/Color.kt` y se acceden a través de `groveColors`.

### Espaciado

Sistema basado en múltiplos de 4dp:
- 4dp: Extra Small
- 8dp: Small
- 12dp: Medium-Small
- 16dp: Medium
- 24dp: Large
- 32dp: Extra Large

### Border Radius

- 8dp: Botones pequeños
- 12dp: Botones estándar
- 16dp: Tarjetas
- 24dp: Modales, diálogos

## 🔧 Comandos de Desarrollo

### Configuración Inicial

```bash
# Sincronizar dependencias
./gradlew build

# Limpiar build
./gradlew clean
```

### Compilación y Ejecución

```bash
# Compilar el proyecto
./gradlew assembleDebug

# Instalar en dispositivo/emulador
./gradlew installDebug

# Ejecutar pruebas unitarias
./gradlew test

# Ejecutar pruebas instrumentadas
./gradlew connectedAndroidTest
```

### Linting y Formato

```bash
# Verificar formato de código (ktlint)
./gradlew ktlintCheck

# Formatear código automáticamente
./gradlew ktlintFormat
```

### Análisis de Código

```bash
# Ejecutar análisis estático
./gradlew lint
```

## 📝 Convenciones de Código

### Nomenclatura

- **Clases**: PascalCase (ej: `TimerViewModel`, `SleepTimerService`)
- **Funciones**: camelCase (ej: `startTimer()`, `updateMediaInfo()`)
- **Constantes**: UPPER_SNAKE_CASE (ej: `ACTION_TIMER_UPDATE`)
- **Variables**: camelCase (ej: `timerState`, `remainingTime`)

### Estructura de Archivos

- Un archivo por clase/interfaz
- Los archivos deben estar en el paquete correspondiente a su ubicación física
- Los componentes Compose deben estar en `ui/components/` o `ui/common/`

### Comentarios

- Usar comentarios KDoc para clases públicas y funciones importantes
- Comentarios en español para explicar lógica compleja
- Evitar comentarios obvios que solo repiten el código

### Imports

- Organizar imports por grupo:
  1. Imports de Android
  2. Imports de AndroidX
  3. Imports de Jetpack Compose
  4. Imports del proyecto (com.thecodegrove.grovetimer)
  5. Imports de librerías externas

## 🔐 Permisos y Servicios

### Permisos Requeridos

La aplicación requiere los siguientes permisos (definidos en `AndroidManifest.xml`):

- `FOREGROUND_SERVICE`: Para ejecutar servicios en primer plano
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK`: Para controlar reproducción multimedia
- `POST_NOTIFICATIONS`: Para mostrar notificaciones
- `MEDIA_CONTENT_CONTROL`: Para controlar contenido multimedia
- `VIBRATE`: Para vibración al finalizar temporizador
- `MODIFY_AUDIO_SETTINGS`: Para modificar configuración de audio

### Servicios

- **SleepTimerService**: Servicio foreground que gestiona el temporizador
- **TimerNotificationService**: Gestiona notificaciones persistentes
- **MyNotificationListener**: Necesario para acceder a MediaSessionManager

## 🧪 Testing

### Estructura de Tests

```
app/src/
├── test/          # Pruebas unitarias
└── androidTest/   # Pruebas instrumentadas
```

### Ejecutar Tests

```bash
# Todas las pruebas unitarias
./gradlew test

# Pruebas instrumentadas (requiere dispositivo/emulador)
./gradlew connectedAndroidTest
```

## 📚 Modelos de Dominio

### TimerState

Representa el estado actual del temporizador:
- `Idle`: Sin temporizador activo
- `Running`: Temporizador en ejecución
- `Paused`: Temporizador pausado
- `Finished`: Temporizador completado

### UserSettings

Configuraciones del usuario:
- `fadeoutEnabled`: Habilitar fadeout al finalizar
- `vibrateOnFinish`: Vibrar al finalizar
- `darkModeEnabled`: Modo oscuro
- `defaultTimerDuration`: Duración por defecto
- `fadeoutDuration`: Duración del fadeout
- `soundEnabled`: Habilitar sonido
- `hapticFeedbackEnabled`: Habilitar feedback háptico

### MediaInfo

Información sobre el contenido multimedia actual:
- `title`: Título de la canción/video
- `artist`: Artista
- `isPlaying`: Si está reproduciéndose

## 🔄 Flujo de Trabajo de Desarrollo

### 1. Antes de Empezar

1. Leer y entender este documento (`AGENTS.md`)
2. **Consultar el archivo `TODO.md`** para ver si la tarea solicitada ya está documentada
3. Revisar la estructura del proyecto
4. Verificar que las dependencias estén sincronizadas: `./gradlew build`

### 2. Trabajo con el TODO.md

**IMPORTANTE**: El archivo `TODO.md` contiene todas las tareas pendientes de la aplicación.

#### 2.1. Al recibir una tarea

1. **Siempre consultar primero `TODO.md`** para verificar si la tarea ya está documentada
2. Si la tarea está en el TODO:
   - Leer la descripción completa y los archivos relacionados
   - Seguir las instrucciones y sugerencias de implementación
   - Trabajar en la tarea según la documentación
3. Si la tarea NO está en el TODO:
   - Informar al usuario que la tarea no está documentada
   - Preguntar si se debe añadir al TODO antes de comenzar

#### 2.2. Al completar una tarea del TODO

**CRÍTICO**: **NUNCA marcar una tarea como completada automáticamente**. El proceso es:

1. Completar la implementación de la tarea
2. Verificar que funciona correctamente
3. **Esperar confirmación explícita del usuario** antes de marcar como completada
4. Solo cuando el usuario dé el OK explícito, marcar la tarea en el TODO con `[x]`
5. Si el usuario no confirma, dejar la tarea sin marcar

**Formato para marcar tareas completadas:**
- Cambiar `- [ ]` por `- [x]` en las subtareas
- Si toda la tarea principal está completa, se puede añadir una nota al final

**Ejemplo:**
```markdown
### 3. TopBar claro: iconos de notificaciones no se ven

**Testing:**
- [x] Verificar visibilidad de iconos en modo oscuro con TopBar claro
- [x] Comprobar contraste de iconos
- [x] Verificar en diferentes dispositivos

**Estado:** ✅ Completado - [Fecha de finalización]
```

### 3. Durante el Desarrollo

1. Seguir las convenciones de código establecidas
2. Usar componentes existentes cuando sea posible
3. Mantener la separación de capas (data, domain, ui)
4. Escribir código limpio y bien comentado
5. Si trabajas en una tarea del TODO, seguir las sugerencias de implementación documentadas

### 4. Antes de Commit

1. Ejecutar linting: `./gradlew ktlintCheck`
2. Ejecutar tests: `./gradlew test`
3. Verificar que el proyecto compile: `./gradlew assembleDebug`
4. Revisar cambios con `git status` y `git diff`
5. **NO marcar tareas del TODO como completadas** hasta que el usuario lo confirme

### 5. Manejo de Errores

Si cualquier comando falla:
1. Detener inmediatamente la tarea actual
2. Reportar el comando ejecutado y su salida completa
3. No intentar arreglar el problema sin instrucciones explícitas del usuario

## 🚀 Características Principales

### Temporizador

- Establecer temporizador con duración personalizable
- Pausar/reanudar temporizador
- Detener temporizador
- Recuperar estado del temporizador al reiniciar la app

### Control Multimedia

- Detectar contenido multimedia activo
- Pausar automáticamente al finalizar temporizador
- Mostrar información del contenido actual

### Configuraciones

- Modo oscuro/claro
- Fadeout al finalizar
- Vibración al finalizar
- Sonido al finalizar
- Feedback háptico

## 📖 Recursos Adicionales

- **Style Guide**: `design/code_grove_style_guide.html`
- **Documentación del Proyecto**: `AGENTS.md` (raíz del proyecto)
- **Android Developer Docs**: https://developer.android.com
- **Jetpack Compose Docs**: https://developer.android.com/jetpack/compose

## 🌐 Localización y Strings

### Idiomas Soportados

La aplicación soporta los siguientes idiomas:
- **Español** (por defecto): `app/src/main/res/values/strings.xml`
- **Catalán**: `app/src/main/res/values-ca/strings.xml`
- **Inglés**: `app/src/main/res/values-en/strings.xml`

### Regla Crítica: Traducción de Strings

**IMPORTANTE**: Cada vez que se añade un nuevo string a la aplicación, **DEBE traducirse a TODOS los idiomas disponibles**.

#### Proceso Obligatorio al Añadir un String:

1. **Añadir el string al archivo base** (`values/strings.xml`) en español
2. **Añadir la traducción al catalán** en `values-ca/strings.xml`
3. **Añadir la traducción al inglés** en `values-en/strings.xml`
4. **NUNCA dejar un string sin traducir** en alguno de los idiomas

#### Ejemplo de Implementación:

```xml
<!-- values/strings.xml (Español) -->
<string name="new_feature_title">Nueva Funcionalidad</string>

<!-- values-ca/strings.xml (Catalán) -->
<string name="new_feature_title">Nova Funcionalitat</string>

<!-- values-en/strings.xml (Inglés) -->
<string name="new_feature_title">New Feature</string>
```

#### Uso en Código:

- **En componentes Compose**: Usar `stringResource(R.string.string_name)`
- **En servicios/actividades**: Usar `getString(R.string.string_name)`
- **NUNCA usar strings hardcodeados** directamente en el código

#### Verificación:

Antes de completar cualquier tarea que añada strings:
- [ ] Verificar que el string existe en `values/strings.xml`
- [ ] Verificar que existe la traducción en `values-ca/strings.xml`
- [ ] Verificar que existe la traducción en `values-en/strings.xml`
- [ ] Verificar que no hay strings hardcodeados en el código

### Estructura de Archivos de Strings

Los archivos de strings están organizados por secciones:
- App Name
- Main Screen
- Settings Screen
- Permissions
- Notifications
- Timer Display
- Media Info
- etc.

Mantener esta organización al añadir nuevos strings.

## ⚠️ Notas Importantes

1. **Paquete**: El paquete base es `com.thecodegrove.grovetimer`.
2. **Servicios**: Los servicios deben ejecutarse como foreground services para funcionar correctamente
3. **Permisos**: La aplicación requiere permisos especiales que el usuario debe conceder manualmente
4. **MediaSessionManager**: Requiere que el usuario habilite el acceso a notificaciones en los ajustes del sistema
5. **Localización**: Todos los strings deben estar traducidos a los 3 idiomas soportados (Español, Catalán, Inglés)

---

**GroveTimer** - Temporizador de Contenido Multimedia con Pausa Automática
