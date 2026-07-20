---
name: tramo-motion
description: >
  Principios de animación, movimiento y convenciones de UI para Tramo (app de sesiones de estudio
  pomodoro). Aplica esta skill SIEMPRE que trabajes en Tramo: UI, transiciones, gestos, el temporizador,
  contadores, feedback de botones, el widget, ajustes, o cualquier elemento animado o visual. Codifica
  los principios de motion de Emil Kowalski adaptados a Tramo, su paleta (clara y oscura), y las reglas
  de trabajo del proyecto.
---

# Tramo · Motion y convenciones de UI

> Skill ESPECÍFICA de Tramo. Vive en el repo de Tramo (`.claude/skills/tramo-motion/`). Las skills
> genéricas de animación de Emil viven aparte, en `~/.claude/skills/` (globales): `emil-design-eng`,
> `review-animations`, `improve-animations`, `animation-vocabulary`, `apple-design`. Esta skill las
> COMPLEMENTA con lo propio de Tramo; no las reemplaza.

## Principios de motion (Emil Kowalski, adaptados a Compose)
La animación sirve a la COMPRENSIÓN, no al adorno. Reglas que se respetan siempre:
- Toda animación tiene un PROPÓSITO: guiar la atención, dar continuidad espacial, o confirmar una acción.
  Si no ayuda a entender qué pasó, sobra.
- Movimiento NATURAL: curvas físicas (spring/ease apropiados), nunca lineales para movimiento perceptible.
  Sin rebotes gratuitos; asentamiento calmado.
- Duraciones cortas y honestas: microinteracciones ~150-250 ms; transiciones de pantalla algo más.
- El TEMPORIZADOR es el corazón de Tramo: su animación debe ser precisa y CALMADA, favorecer la
  concentración y nunca distraer. Evita animaciones continuas o llamativas que compitan con el foco del
  usuario durante una sesión de estudio.
- Respeta SIEMPRE el movimiento reducido del sistema: degrada a cambios directos/fades.
- El motion no bloquea la interacción ni retrasa el feedback: el usuario manda, la animación acompaña.
- Coherencia: el mismo tipo de elemento se anima igual en toda la app.

## Identidad visual de Tramo — PALETA (fuente de verdad, no inventar colores)
Definida en `com.luis.tramo.ui.theme`. Tramo tiene TEMA CLARO Y OSCURO: usa siempre el token/rol
adecuado al esquema activo, no un color fijo.

Marca (pine-teal):
- Pine (marca):            #2F5D62  (TramoPine)
- Pine container:          #C9E4E7  (TramoPineContainer)
- On pine container:       #12191A  (TramoOnPineContainer)
- Pine (variante oscuro):  #8FCBD1  (TramoPineDark)         ← más claro, legible sobre superficie oscura
- Pine container oscuro:   #14464B  (TramoPineContainerDark)

Neutros / superficies:
- Fondo claro:             #F5F3EE  (TramoBackgroundLight)
- Fondo oscuro:            #12191A  (TramoBackgroundDark)
- Tinta (onSurface claro): #1A2224  (TramoInk)
- Error:                   #B4534B  (TramoError)

Rol de PRIORIDAD BAJA (slate-blue, distinto del verde de marca; se invierte claro/oscuro):
- Container oscuro:        #3A4A5A  (LowSlateContainerDark)   / On: #D3E1EF
- Container claro:         #D4E0EC  (LowSlateContainerLight)  / On: #1D2A36

Acento de PROGRESO (ámbar) — REGLA ESTRICTA:
- Progreso:                #E8B75D  (TramoProgress)
- Se usa EXCLUSIVAMENTE para racha, hitos y progreso. NUNCA como color de marca, ni como relleno de
  botón, ni como acento primario. El color primario/de marca es el pine-teal, no el ámbar. Respeta esta
  separación: es una convención deliberada del proyecto.

Paleta de personalización de TARJETAS DE TAREA (swatches; ARGB Long, persistidos en TaskEntity):
- Indigo #6366F1 · Teal #0EA5A4 · Amber #F59E0B · Red #EF4444 · Green #10B981 · Blue #3B82F6 ·
  Pink #EC4899 · Violet #8B5CF6 · Slate #64748B · Orange #F97316
- Son colores ELEGIBLES por el usuario para sus tarjetas; no son colores de la UI global de la app.

Regla general de color: el pine-teal es el protagonista de marca; el ámbar solo para progreso; fondos y
texto en la escala neutra según el esquema (claro/oscuro). No introduzcas colores fuera de estos tokens.

## Arquitectura de Tramo
- Patrón: MVVM + Repository, con lógica de dominio pura y aislada (`computeCurrentStreak`,
  `nextSessionType`: sin dependencias de Android → deterministas y testeables). Estado unidireccional
  expuesto como `StateFlow` desde los ViewModels.
- Estructura de paquetes (bajo `com.luis.tramo`):
  - `data/` — Room (`session/`: `SessionDao`, `SessionRecordEntity`, `SessionRepository`, `Streaks`;
    `task/`: `TaskDao`, `TaskEntity`, `TaskRepository`, `TaskConverters`, `TramoDatabase`) y
    `UserPreferencesRepository` (DataStore).
  - `di/` — `DatabaseModule`, `DataStoreModule` (Hilt).
  - `timer/` — `PomodoroTimerService`, `TimerAlarmScheduler`, `TimerAlarmReceiver`, `TimerStateHolder`,
    `TimerState`, `SessionType`.
  - `ui/` — pantallas Compose + ViewModels: `timer/`, `tasks/`, `report/`, `settings/`, `onboarding/`,
    más `components/` (`ScreenEntrance`, `StatCard`) y `theme/` (`Color`, `Theme`, `Type`, `Spacing`).
  - `navigation/` — `TramoDestinations`, `TramoNavHost`, `TramoShell`.
  - `widget/` — `TramoFocusWidget` (Glance), `TramoWidgetReceiver`, `WidgetSnapshotProvider`,
    `WidgetUpdater`, `WidgetPinner`.
- Stack: Kotlin · Jetpack Compose · Material 3 · Hilt (DI, con KSP) · Room · DataStore · Jetpack Glance
  (widget) · Coroutines/Flow · Foreground Service + AlarmManager. 100 % offline (sin permiso `INTERNET`).
- Temporizador hoy: `PomodoroTimerService` es un foreground service (`specialUse`, subtipo
  `pomodoro_timer`) anclado a un **deadline absoluto** respaldado por una **alarma exacta**
  (`TimerAlarmScheduler` → `TimerAlarmReceiver`), no por un contador en memoria. El servicio persiste un
  snapshot de la sesión activa en DataStore; al arranque en frío el ViewModel lo restaura y reprograma la
  alarma. `TimerStateHolder` (singleton) expone el estado como `StateFlow`; la UI envía acciones al
  servicio vía intents. Ciclo: concentración → descanso corto → descanso largo cada N sesiones, con
  auto-inicio opcional.
- Convenciones ya establecidas:
  - Ajustes escribe en DataStore **al instante** (no hay botón Guardar); cada setter es un
    `viewModelScope.launch`.
  - Invariante mantenida en el ViewModel, no solo en la UI (ej.: descanso largo ≥ descanso corto).
  - Entrada de pantalla escalonada con `ScreenEntrance(index, visible, reduceMotion)` y
    `rememberReduceMotion()` — respeta movimiento reducido; úsalo en pantallas nuevas.
  - Espaciado siempre por tokens `Spacing.xs/sm/md/lg/xl`; nunca dp sueltos arbitrarios.
  - Cifras que cambian en vivo con `TabularFigures` (evita el baile de anchos).
  - Ajustes se compone de `SectionCard(eyebrow, icon)` + filas `PreferenceRow(icon, title, subtitle,
    onClick, trailing)`; los controles de cola son `Switch`, `Stepper` o `ValueChevron`.
  - i18n: 6 idiomas (`values`, `-es`, `-de`, `-fr`, `-ja`, `-pt`) + `values-night`. **Todo texto visible va
    a `strings.xml`**, nunca hardcodeado; cambio de idioma en runtime con
    `AppCompatDelegate.setApplicationLocales`.

## Widget de Tramo (Glance)
- Si el widget usa Glance: datos reactivos vía `provideContent` + Flow (recompone solo).
- OJO MIUI/Xiaomi: el launcher CACHEA los ImageView del RemoteViews; el texto nativo se refresca, las
  imágenes (bitmaps) pueden no repintarse. Prefiere texto/formas nativas si algo no se actualiza en Xiaomi.
- Dedup de PendingIntents: usa una `action` distinta por destino (los extras no diferencian).
- Anclaje: `requestPinAppWidget` con comprobación de `isRequestPinAppWidgetSupported`. Idempotencia al
  anclar: comprobar `getAppWidgetIds(ComponentName)` antes de relanzar el flujo (no duplicar).
- Respeta el tema claro/oscuro también en el widget si aplica.
- Detalles reales del widget de Tramo:
  - `TramoFocusWidget : GlanceAppWidget`, registrado por `TramoWidgetReceiver : GlanceAppWidgetReceiver`
    (declarado en el manifiesto con `@xml/tramo_focus_widget_info`). **Ese receiver es el `ComponentName`
    del provider** para `requestPinAppWidget` / `getAppWidgetIds`.
  - Contenido: anillo de progreso del día que se torna ámbar (`TramoProgress`) al cumplir la meta diaria;
    al tocarlo hace deep-link a Ajustes resaltando la fila de meta diaria (`highlight = "daily_goal"`).
  - Datos: `WidgetSnapshotProvider` lee los repositorios vía Hilt `EntryPoint`, y el Flow se recolecta
    **dentro de `provideContent`** (un snapshot leído una sola vez quedaría congelado).
  - Repintado: SIEMPRE a través de `WidgetUpdater.refresh()` (singleton, `debounce(250 ms)` →
    un solo `updateAll`). No llames a `updateAll` directamente: el rate-limit del AppWidgetHost en
    MIUI/HyperOS descarta ráfagas y deja el widget desfasado.
  - Anclaje: `WidgetPinner` (inyectable, `@ApplicationContext`) encapsula el flujo de pin. Se invoca al
    terminar el onboarding y desde Ajustes.
  - Tamaño: 2x1 por defecto, redimensionable (110×48 dp mínimo).

## Reglas de trabajo (SIEMPRE, en todo cambio de Tramo)
- NO rompas lo que ya está definido y funciona. Si un cambio pareciera tocar algo estable, PREGUNTA antes.
- Cambios acotados a la tarea pedida; nada de refactors no solicitados.
- Respuestas BREVES.
- Control de versiones: NO ejecutar git. Entrega los comandos de commit en el chat, ATÓMICOS, en español,
  convencionales (feat/fix/style), SIN co-autoría, SIN "Generated with Claude Code", SIN push. Commits AL
  FINAL, solo cuando el usuario confirme que el cambio quedó bien.
- Ciclo de prueba en CADA cambio relevante: desinstalar el APK actual, compilar, reinstalar el APK de
  desarrollo, y verificar en el dispositivo antes de dar el cambio por bueno.