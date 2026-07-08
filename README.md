# ⏳ Tramo

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Material 3](https://img.shields.io/badge/Material_3-6750A4?style=for-the-badge&logo=materialdesign&logoColor=white)
![Room](https://img.shields.io/badge/Room-003B57?style=for-the-badge&logo=sqlite&logoColor=white)
![Hilt](https://img.shields.io/badge/Hilt-DI-34A853?style=for-the-badge)

## 📌 Descripción

**Tramo** es una app Android nativa de **enfoque (Pomodoro), gestión de tareas y analítica de progreso**, pensada como una sola experiencia fluida y **100 % offline**: sin cuentas, sin red, sin backend. Todo vive en el dispositivo.

Su seña de identidad es un **temporizador confiable** que no se desfasa en segundo plano ni se pierde si el sistema mata el proceso, un **widget de escritorio** con un anillo de progreso reactivo, y una capa de **analítica local** (racha, mapa de calor, actividad mensual).

**Stack:** Kotlin · Jetpack Compose · Material 3 · Hilt (DI) · Room · DataStore · Jetpack Glance (widget) · Coroutines/Flow · Foreground Service + AlarmManager.

---

## Problema que resuelve

Las apps de productividad suelen fallar justo donde más importa:

- **Timers que se rompen en segundo plano:** cuentan en memoria y se desfasan o se reinician cuando el SO congela/mata el proceso.
- **Dependencia de cuentas y red** para algo intrínsecamente personal y local, con la latencia y las dudas de privacidad que eso implica.
- **Herramientas fragmentadas:** el temporizador, las tareas y las estadísticas viven en apps distintas.

Tramo resuelve esto con un **temporizador anclado a un deadline absoluto** (respaldado por alarma exacta y persistencia), y unifica timer + tareas + progreso en un flujo único, **sin conexión y sin fricción**.

---

## Responsabilidades principales

- **Temporizador Pomodoro** en un *foreground service*: ciclo concentración → descanso corto → descanso largo (cada N sesiones), configurable, con auto-inicio opcional.
- **Supervivencia a la muerte de proceso:** la sesión activa se restaura desde un snapshot persistido y una alarma exacta autoritativa.
- **Gestión de tareas:** subtareas con *rollup*, prioridad (con distintivo visual), recurrencia, etiquetas; swipe para completar/eliminar con *undo* y retroalimentación.
- **Analítica local:** racha de días, mapa de calor de 10 semanas y actividad mensual (6 meses), computadas en la capa de dominio.
- **Widget de escritorio (Glance):** anillo de progreso reactivo que se torna ámbar al cumplir la meta y abre Ajustes al tocarlo.
- **Celebración de meta diaria:** momento in-app (el anillo "florece") una vez al día al alcanzar el objetivo.
- **Internacionalización:** 6 idiomas con cambio de idioma per-app en runtime.

---

## Flujo general

**Ciclo de una sesión**

```
Concentración ──(termina)──► Descanso corto ──► Concentración ──► ...
       │
       └── cada N concentraciones ──► Descanso largo ──► (reinicia el ciclo)
```

**Cómo se comunican los componentes**

```
        acciones (play/pause/skip)        estado (StateFlow)
  UI (Compose + ViewModels) ───────► PomodoroTimerService ───► TimerStateHolder ──► UI
                                             │
                     deadline absoluto + alarma exacta (AlarmManager)
                                             │
                                             ▼
                     DataStore  ◄── snapshot de sesión activa ──►  restauración tras
                                                                    muerte de proceso
  Room (sesiones, tareas) ──► Repositorios ──► ViewModels
        ▲                                          │
        └── al completar sesión ──► WidgetUpdater (debounce) ──► Glance Widget
```

---

## Estructura interna

**Capas (paquetes bajo `com.luis.tramo`)**

- `data/` — Room (DAOs, entidades), DataStore y repositorios; capa de persistencia y acceso.
- `timer/` — `PomodoroTimerService` (foreground), programador de alarmas y el *state holder* compartido.
- `ui/` — pantallas Compose y ViewModels (`timer`, `tasks`, `report`, `settings`, `onboarding`).
- `widget/` — widget Glance, proveedor de snapshot reactivo y actualizador con *debounce*.
- Lógica de dominio pura y aislada: `computeCurrentStreak`, `nextSessionType` (sin dependencias de Android → deterministas y testeables).

**Persistencia**

- **Room:** `SessionRecordEntity` (tipo, duración, `completedAt`) y `TaskEntity` (subtareas, prioridad, etiquetas, recurrencia, categoría, color).
- **DataStore:** preferencias del usuario, **snapshot de la sesión activa** (para reconstruir el timer tras un reinicio del proceso) y control de la celebración diaria.

---

## Comunicación con otros servicios

Tramo es una **app autónoma y offline**: no consume APIs ni declara permiso de `INTERNET`. La "comunicación" relevante es **interna**, entre componentes que corren en el mismo proceso:

- **Service ↔ UI:** un `TimerStateHolder` singleton expone el estado como `StateFlow`; la UI envía acciones al servicio vía intents.
- **Recuperación de estado:** el servicio persiste un snapshot en DataStore; al arrancar en frío, el ViewModel lo restaura y reprograma la alarma.
- **App ↔ Widget:** el widget Glance lee los mismos repositorios mediante un *Hilt EntryPoint* y se refresca a través de un `WidgetUpdater` con `debounce`; el toque en el anillo hace *deep-link* a Ajustes con extras en el intent.

---

## Decisiones técnicas

- **Deadline absoluto + AlarmManager exacto** en lugar de un contador en memoria: el tiempo no se desfasa en segundo plano y la finalización se dispara aunque el proceso muera.
- **Foreground service** (`specialUse`) para mantener el conteo vivo y visible mientras corre una sesión.
- **Widget reactivo con Glance:** los datos se recolectan como `Flow` *dentro* de `provideContent`; un snapshot leído una sola vez quedaría congelado.
- **`WidgetUpdater` con `debounce`:** evita que MIUI/HyperOS descarte actualizaciones de widget muy seguidas (rate-limit del *AppWidgetHost*).
- **MVVM + Repository + dominio puro:** estado unidireccional con `StateFlow`, lógica de negocio aislada y probable de testear.
- **Offline-first (Room + DataStore):** privacidad, cero latencia y funcionamiento sin conexión por diseño.
- **Hilt** para inyección de dependencias y **KSP** (en vez de kapt) para el procesamiento de anotaciones de Room/Hilt.
- **i18n en runtime** con `AppCompatDelegate.setApplicationLocales` + `locales_config.xml` (cambio de idioma sin reinstalar).

---

## Desarrollo local

**Requisitos**

- Android Studio (versión reciente) · JDK 17 · Android SDK 37 · dispositivo/emulador con **API 28+**.
- No hace falta configurar secretos ni servicios: al ser offline, la build **debug** funciona directamente tras sincronizar Gradle.

**Ejecutar**

```bash
git clone https://github.com/MrX-zeta/Tramo.git
cd Tramo
# Abre el proyecto en Android Studio, espera la sincronización de Gradle y pulsa Run,
# o desde consola:
./gradlew installDebug   # instala la variante debug en el dispositivo conectado
```

**Generar un APK de release firmado** (la firma vive fuera del repo):

1. Crea un keystore: `keytool -genkey -v -keystore tramo-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias tramo`.
2. Crea en la raíz un `keystore.properties` (git-ignored) con `storeFile`, `storePassword`, `keyAlias`, `keyPassword`.
3. `./gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk`.

---

## Pruebas

No hay aún una suite automatizada (solo los *stubs* de plantilla). El diseño, sin embargo, la habilita: la **lógica crítica está en funciones de dominio puras** (`computeCurrentStreak`, `nextSessionType`) listas para pruebas unitarias, y los ViewModels exponen estado observable fácil de verificar. La validación funcional —timer en segundo plano, widget, celebración, i18n— se realizó en dispositivo físico.

---

## Valor dentro de la arquitectura

Tramo es un **producto autónomo**, no una pieza de un sistema mayor. Su valor técnico —y lo que demuestra como proyecto— está en resolver bien los problemas *difíciles* de una app móvil real:

- **Corrección en segundo plano:** un timer que sobrevive a la gestión agresiva de energía y a la muerte de proceso.
- **Integración con el sistema operativo:** foreground service, alarmas exactas, notificaciones con canal, y un widget de escritorio Glance que se comporta bien incluso bajo el rate-limit de MIUI.
- **Arquitectura limpia y offline-first:** MVVM + Repository, dominio desacoplado, Compose declarativo y persistencia local — mantenible y extensible.

---

*Desarrollado por [Brian Luis Ruiz Pérez (MrX-zeta)](https://github.com/MrX-zeta).*
