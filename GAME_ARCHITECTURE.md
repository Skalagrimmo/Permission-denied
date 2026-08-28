# Permission Denied — Complete Game Architecture & Technical Specification

## 1. Executive System Overview

**Permission Denied** is a high-performance first-person cyberpunk stealth-action and hacking game built natively for Android using **Jetpack Compose**, **OpenGL ES 2.0/3.0**, **Room Persistence**, and a custom **Real-Time Procedural Synthesizer**.

The architecture follows a decoupled **Layered MVVM + ECS-inspired Game Engine** pattern, ensuring strict separation between hardware rendering, game simulation logic, audio generation, data persistence, and reactive user interfaces.

---

## 2. High-Level System Architecture

```
┌────────────────────────────────────────────────────────────────────────────┐
│                       Jetpack Compose UI Layer                             │
│  MainMenuScreen ── DistrictSelectScreen ── GameScreen ── MissionReportScreen│
│  GameHudOverlay ── HackingDialog ── InventoryDialog ── SettingsDialog       │
└─────────────────────────────────────┬──────────────────────────────────────┘
                                      │ UI State / Action Dispatches
┌─────────────────────────────────────▼──────────────────────────────────────┐
│                        GameViewModel (AAC ViewModel)                       │
│  Exposes StateFlow<GameUiState>, Dispatches Events, Manages Coroutine Scope│
└──────────────────┬─────────────────────────────────────┬───────────────────┘
                   │                                     │
┌──────────────────▼──────────────────┐   ┌──────────────▼───────────────────┐
│     Core Game Simulation Engine     │   │      Data & Persistence Layer    │
│  GameEngine.kt                      │   │  GameRepository.kt               │
│  ├── PlayerMovementController       │   │  ├── AppDatabase (Room)          │
│  ├── WorldGenerator (Districts)     │   │  ├── GameSaveDao & HighScoreDao  │
│  ├── HackingGraph (ICE Nodes)       │   │  └── CampaignProgressDao         │
│  └── GameHapticsEngine              │   └──────────────────────────────────┘
└───────────┬──────────────────────┬──┘
            │                      │
┌───────────▼──────────────────┐ ┌─▼─────────────────────────────────────────┐
│  Dual Rendering Pipeline     │ │    Procedural Sound Synthesis Engine      │
│  ├── Hardware OpenGL ES      │ │  GameAudioEngine.kt                       │
│  │   (CyberpunkGlRenderer,   │ │  ├── 16-Bit PCM Procedural Synth          │
│  │    Primitives3D Shaders)  │ │  ├── 16-Step Dynamic Sequencer            │
│  └── Software ASCII Canvas   │ │  └── Cyberpunk SFX Sound Wave Generator   │
│      (AsciiRasterizer, Ramps)│ └───────────────────────────────────────────┘
└──────────────────────────────┘
```

---

## 3. Detailed Component Breakdown

### 3.1. Game Simulation Engine (`com.example.engine`)

The game simulation operates independently from UI drawing frame-rates using delta-time (`simDelta`) physics steps.

- **`GameEngine.kt`**: Central deterministic state machine managing player stats (Health, Energy, Cyberdeck RAM, Noise/Alarm levels), weapon inventories, ammunition, augmentations, active enemy patrols, security cameras, laser tripwires, and objective completion.
- **`PlayerMovementController.kt`**:
  - **Vector Mathematics**: Transforms local joystick/WASD movement vectors (`moveX`, `moveZ`) into camera-aligned 3D world translations based on current yaw heading.
  - **Non-Inverted Camera Look**: Processes screen drag gestures with clamped pitch constraints (`-45°` to `+45°`) and 360° circular yaw rotation.
  - **Continuous Swept Collision**: Evaluates player bounding circle (`radius = 0.28`) against solid AABB grid tiles (`TileType.isSolid`).
  - **Sliding Resolution**: Employs axis-decoupled sliding resolution to prevent snagging against walls and obstacles.
- **`WorldGenerator.kt`**: Procedurally generates atmospheric cyberpunk districts (`DISTRICT_01: Neon Alley`, `DISTRICT_02: Corporate Plaza`, `DISTRICT_03: Data Core`) containing interactive terminals, loot containers, laser fields, patrol paths, and mission extraction points.
- **`Vector3.kt`**: Lightweight 3D linear algebra utility providing vector addition, normalization, cross-products, dot-products, and distance calculations.
- **`GameHapticsEngine.kt`**: Provides tactile vibration feedback for footsteps, weapon recoil, damage impacts, terminal hacking, and stealth breaches.

---

### 3.2. Dual-Pipeline Rendering Subsystem (`com.example.renderer`)

The engine provides two distinct rendering modes selectable dynamically by the player:

#### A. Hardware Accelerated OpenGL ES Pipeline
- **`CyberpunkGlSurfaceView.kt`**: Native Android `GLSurfaceView` embedding the OpenGL renderer into Compose.
- **`CyberpunkGlRenderer.kt`**: Full 3D rendering pipeline:
  - Direct native buffer management (`FloatBuffer`, `IntBuffer`) with zero garbage collection allocations in the render loop.
  - Generates multi-story buildings, holographic neon billboards, volumetric fog, animated laser meshes, and dynamic spot/point lights.
- **`ProceduralLevelMeshGenerator.kt`**: Transforms the 2D ASCII district layout into optimized 3D vertex and index buffers.
- **`FirstPersonCameraController.kt`**: Generates View and Projection matrices using standard perspective frustum geometry.
- **`CyberpunkAsciiPostShader.kt` & `FramebufferHelper.kt`**: Off-screen framebuffer render targets applied with CRT scanlines, chromatic aberration, phosphor persistence, and glitch distortion shaders.

#### B. Real-Time ASCII Software Rasterizer
- **`AsciiRasterizer.kt` & `AsciiRamps.kt`**:
  - Fast raycasting rasterizer mapping 3D geometry and lighting directly into high-density ASCII glyph arrays.
  - Applies cyberpunk color palettes (Matrix Green, Cyber Neon, Amber Terminal, High-Contrast Stealth) with luminance-weighted character ramps (`" .:-=+*#%@"`, `" ░▒▓█"`).

---

### 3.3. Audio & Synthesis Subsystem (`com.example.audio`)

- **`GameAudioEngine.kt`**:
  - Pure software procedural sound synthesizer using native Android `AudioTrack` with streaming 16-bit 44.1kHz PCM audio.
  - **Dynamic Music Sequencer**: Plays 16-step cyberpunk basslines and dark synthwave arpeggios that dynamically transition in tempo and distortion when alarms are triggered.
  - **Procedural Sound Effects**: Generates real-time audio waveforms without static audio asset overhead:
    - Silenced Pistol & Heavy Shotgun gunfire (shaped white noise + low-pass resonant decay)
    - Cyberdeck terminal connection pulses and ICE bypass sweeps
    - Energy shield impacts, cloaking hums, and cybernetic dash bursts.

---

### 3.4. Domain & Data Models (`com.example.model`)

- **`GameModels.kt`**:
  - **`TileType`**: Solid geometry, sight-blocking glass, doors, security barriers, laser tripwires, terminals.
  - **`WeaponType`**: Kinetic silenced pistols, plasma carbines, railguns, EMP shockers with distinct damage, fire rates, spread, and sound profiles.
  - **`AugmentationType`**: Cybernetic implants including Optical Camouflage (Cloaking), Dash Thrusters, Neural ICE Breaker, Thermal Vision, and Threat Analyzer.
  - **`EnemyState` & `SecurityAlertLevel`**: State-machine tracking Patrol, Suspicion, Alert, Combat, and Stunned states.
- **`HackingGraph.kt`**: Multi-node cybersecurity network graph representing corporate mainframes, firewall ICE blocks, encryption nodes, and reward payloads.
- **`HudNotification.kt`**: Event notification queue for stealth warnings, mission objectives, and item acquisitions.

---

### 3.5. Persistence & Database Layer (`com.example.data`)

- **`AppDatabase.kt`**: SQLite database configured via **Room**:
  - `GameSaveEntity`: Saves player coordinates, inventory, augmentations, district index, and active mission timers.
  - `HighScoreEntity`: Tracks top stealth runs, hacks completed, credits stolen, and time efficiency.
  - `CampaignProgressEntity`: Tracks unlocked districts, cyberdeck upgrades, and permanent perks.
- **`GameRepository.kt`**: Singleton data gateway managing thread-safe coroutine persistence (`Dispatchers.IO`) with safe Room database schema migration support.

---

### 3.6. User Interface & Presentation Layer (`com.example.ui`)

- **`GameViewModel.kt`**: Manages unidirectional state flow (`StateFlow<GameUiState>`), coordinating game loop lifecycle, audio engine state, database operations, and dialog visibility.
- **`GameScreen.kt`**: Main gameplay container hosting the 3D surface view, HUD overlay, notifications, and interaction dialogs.
- **`GameHudOverlay.kt`**:
  - Responsive analog touch joystick and directional aiming swipe zones.
  - Physical WASD + Keyboard hotkey support (`W/A/S/D`, `Shift` to sprint, `C` to crouch, `Space` to dash, `E` to interact, `R` to reload, `F` for quick-hack, `I` for inventory).
  - Dynamic cyberdeck status indicators, compass navigation tape, ammo counter, and health/energy gauges.
- **`HackingDialog.kt`**: Interactive matrix terminal mini-game to bypass security nodes, disable cameras, and siphon corporate credits.
- **`InventoryDialog.kt` & `AugmentationsSelectScreen.kt`**: Loadout customization for weapons, cyberdeck programs, and cybernetic upgrades.

---

## 4. Input & Control Mapping

| Action | Touch Control | Keyboard Control |
| :--- | :--- | :--- |
| **Move / Strafe** | Left Virtual Joystick | `W`, `A`, `S`, `D` / Arrow Keys |
| **Look / Aim** | Right Screen Swipe | Mouse / Swipe |
| **Sprint** | Sprint Button | `Left Shift` / `Right Shift` |
| **Crouch / Stealth** | Crouch Button | `C` / `Left Ctrl` |
| **Cybernetic Dash** | Dash Button | `Spacebar` |
| **Interact / Hack** | Contextual Action Button | `E` / `Enter` |
| **Reload Weapon** | Ammo HUD Card | `R` |
| **Remote Quick-Hack**| Quick-Hack Deck Button | `F` |
| **Inventory / Loadout**| Backpack HUD Icon | `I` / `Tab` |
| **Pause Menu** | Pause Button | `Escape` |

---

## 5. Performance, Memory & Zero-Allocation Guarantees

1. **Zero Render Allocations**: Both OpenGL ES and ASCII rasterizer pipelines reuse pre-allocated primitive vertex arrays and string/character buffers, avoiding JVM GC frame stutters.
2. **Direct Frame Pacing**: Game update loop leverages Compose `withFrameNanos` synchronized to the display refresh rate with delta-time clamping (`0.001s` - `0.05s`).
3. **Safe Async Storage**: Database queries utilize Kotlin Coroutine `Flow` and Room background workers to ensure the main thread never experiences I/O blocks.
