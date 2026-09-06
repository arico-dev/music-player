# Guía del proyecto: Music Player

Aplicación Android de reproducción de música local, escrita en **Kotlin + Jetpack Compose (Material 3)**, con **Media3/ExoPlayer** como motor de reproducción, **Hilt** para inyección de dependencias y **Room** como caché local.

## Stack

| Área | Tecnología |
|------|------------|
| UI | Jetpack Compose, Compose BOM `2026.02.01`, Material 3 |
| Playback | Media3 ExoPlayer `1.7.1` |
| DI | Hilt `2.60.1` + hilt-navigation-compose |
| Caché | Room `2.8.4` |
| Imágenes | Coil `2.7.0` (con SVG) |
| Red | Retrofit `2.11.0` + OkHttp `4.12.0` + kotlinx.serialization `1.9.0` |
| Build | AGP `9.3.2`, Kotlin `2.2.10`, Gradle 9.5; `minSdk 30`, `targetSdk 36` |

## Jerarquía de código

```
app/src/main/java/com/musicplayer/app/
├── MainActivity.kt            Entrada: crea el host de Hilt y setContent
├── MusicPlayerApp.kt          Application (Hilt)
├── MusicPlayerAppRoot.kt      Navigation + Scaffold principal (bottom bar, miniplayer, rutas)
├── RootViewModel.kt           Acceso global al playbackController + color dominante
├── core/
│   ├── model/                 Album, Song, SongMetadata, LrcLine
│   ├── util/                  AlbumArtColorExtractor, LrcParser
│   └── (la UI theme vive en ui/theme/)
├── data/
│   ├── local/                 Room: MusicDatabase, Entities.kt (songs/albums/artists/genres/song_genres/playlists/playlist_songs)
│   │   └── dao/               AlbumDao, ArtistDao, GenreDao, SongDao, SongGenreDao, PlaylistDao, PlaylistSongDao
│   ├── mediastore/            MediaStoreScanner (sonidos/albumes/generos), MetadataReader y TagReader (tags FLAC/OGG/MP3/MP4)
│   ├── lyrics/                LrcLibApi (Retrofit) + LyricsRepository (normalización y selección)
│   ├── repository/            LibraryRepository (capa que combina Room + MediaStore)
├── di/                        AppModule: Room, OkHttp/Retrofit (LRCLIB), JSON
├── feature/
│   ├── common/                SongInfoSheet (ficha técnica)
│   ├── home/                  HomeScreen
│   ├── library/               LibraryScreen + LibraryViewModel (tabs Canciones/Álbumes/Artistas/Géneros/Carpetas/Playlists),
│   │                          PlaylistsList (+ picker de playlist) y PlaylistDetailScreen + PlaylistDetailViewModel
│   │                          LibraryDetailScreen + LibraryDetailViewModel (detalle genérico)
│   ├── miniplayer/            MiniPlayer (barra sobre el bottom bar)
│   ├── player/                PlayerScreen + PlayerViewModel (reproductor + letras)
│   ├── search/                SearchScreen + SearchViewModel (búsqueda local)
│   └── settings/              SettingsScreen
├── player/                    PlaybackController (MediaSession + ExoPlayer, cola, estados)
└── ui/theme/                  Color.kt, Theme.kt, Type.kt
```

## Navegación (MusicPlayerAppRoot.kt)

- Rutas: `home`, `library`, `search`, `settings`, `player`, y los detalles `album/{id}`, `artist/{id}`, `genre/{id}`, `folder?folderPath=...`, `playlist/{playlistId}`.
- `fullScreenRoutes` = `{PLAYER, ALBUM, ARTIST, GENRE, FOLDER, PLAYLIST}`; al estar en ellas se oculta bottom bar y miniplayer.
- Un solo `LibraryDetailScreen` + `LibraryDetailViewModel` sirve a álbum/artista/género/carpeta (el argumento decide el filtro en el repositorio).
- Cambio de tab inferior usa `popUpTo(startDestination){ saveState=true }` + `launchSingleTop` + `restoreState=true`.

## Workflow de build y despliegue

```bash
# Java (imprescindible: el PATH por defecto no lo encuentra)
export JAVA_HOME=$HOME/.local/share/mise/installs/java/temurin-latest

# Compilar
./gradlew :app:assembleRelease
# APK de salida:
#   app/build/outputs/apk/release/app-release.apk

# Instalar en la tablet de pruebas
ADB=$HOME/Android/Sdk/platform-tools/adb
$ADB install -r app/build/outputs/apk/release/app-release.apk
```

Tablet de pruebas: **e29d45457d84**, 800x1340 px, densidad 213 (≈600x1005 dp). La build release no es debuggable (sin `run-as`): para inspeccionar la base Room hay que instalar la variante `debug`:

```bash
./gradlew :app:installDebug
$ADB shell run-as com.musicplayer.app cat databases/music_player.db > /tmp/mp.db
# el debug se elimina después con la instalación release
```

Orientación forzada (la tablet usa rotación manual):
```bash
$ADB shell settings put system accelerometer_rotation 0
$ADB shell settings put system user_rotation 1   # 1 = landscape, 0 = portrait
```

## Verificación en dispositivo

Flujo habitual de chequeo sin depuración:

1. `adb exec-out screencap -p > cap.png` para capturar pantalla.
2. `adb shell uiautomator dump` + `adb shell cat /sdcard/window_dump.xml` para leer el árbol de accesibilidad (bounds de textos/nodos clicables).
3. Taps por coordenadas sobre los bounds reales (cambian según orientación).
4. Para análisis de píxeles (colores, píldoras seleccionadas, áreas dibujadas): decodificador PNG de stdlib en `/tmp/opencode/pngdecode.py` (no hay PIL en el entorno).

Notas prácticas:
- UI binaria del reproductor: cerrar la hoja de letras = tap en `(44,85)` (icono), el título del header no es clicable.
- La librería se re-escanea en cada arranque en frío (`LibraryViewModel.loadLibrary`, al concederse el permiso).

## Datos y MediaStore

- `MediaStoreScanner.scanSongs` filtra `IS_MUSIC != 0`; los álbumes/géneros se obtienen de sus URIs propias.
- **Géneros**: en esta ROM (MIUI) `content://media/.../genres/<id>/members` reporta `_ID` = id del género, no el audio id. Por eso los miembros se reconstruyen a partir de la columna `GENRE` de la tabla de audio (agrupando por nombre normalizado). Usar siempre ese camino, no `Members`.
- `artistId` = `hashCode()` del nombre del artista; `albumId` proviene de MediaStore.


## Feature: Letras sincronizadas (LRCLIB)

- Base URL: `https://lrclib.net/`. Retrofit + OkHttp en `di/AppModule`.
- OkHttp inyecta `User-Agent` de navegador real: Cloudflare responde 520 con el UA por defecto de OkHttp.
- `/api/get?artist_name=X&track_name=Y` devuelve 404 si no existe → el endpoint se declara como `Response<LrcLibLyrics>` (si se declara el tipo directo, Retrofit lanza `HttpException` e impide el fallback a `/search`).
- `/api/search?q=...` es **solo GET** (POST → 405), con `@Query("q")`.
- Normalización de búsqueda (`LyricsRepository.clean`): conservar letras unicode (`\p{L}`, ej. "COMË N GO"), eliminar símbolos/puntuación. Un regex demasiado agresivo rompe acentos y devuelve letras de otra canción.
- Selección (`LyricsRepository`): el `/api/get` exacto puede devolver un registro con solo `plainLyrics` aunque exista la versión sincronizada en `/search`. Regla: si el GET devuelve synced → se usa; si no, `search(título)` con `bestMatch` que prioriza álbum+artista+sync → álbum+sync → álbum+artista → álbum → artista+sync, SIEMPRE con fallback al registro exacto y SIN quedarse con un sync de otra canción (títulos comunes tipo "Smoke" se contaminan en el search).
- UI: colores adaptados al cover del álbum **oscurecidos** (no usar el tema base); la línea activa es una píldora en color del cover aclarado. El fondo de la pantalla usa el cover aún más oscuro. Tap sobre una línea → seek.

## Feature: Playlists

- Tab propio en Biblioteca (`PlaylistsList`): lista + FAB + diálogo de nombre. `PlaylistPickerSheet` (ModalBottomSheet) se abre desde el ⋮ de una canción. `PlaylistDetailScreen` habilita Reproducir, renombrar (nombre pre-rellenado), eliminar (confirmación), quitar canciones, reordenar ↑/↓ y "Agregar canciones" multi-selección con buscador + contador.
- `PlaylistSongDao.insertAll` usa `ON CONFLICT REPLACE`: re-agregar una canción ya presente la mueve al final (dedup de facto, comportamiento aceptado).
- Orden por `position`; el reordenamiento reescribe toda la lista con posiciones consecutivas.

## Metadata (artista principal / álbum)

- MediaStore expone el ÚLTIMO artista de tags multi-artista (`ARTIST=Skrillex;Isoxo;Cristale;TeeZandos` → "TeeZandos"), seccionando álbumes a artistas fantasma. `TagReader` parsea la metadata del archivo y `primary()` = primer artista antes de `;`/`,`/`/`feat`.
- `MediaStoreScanner.scanSongs` devuelve `SongScan(song, albumArtist)`; `LibraryRepository.refresh` deriva los artistas del artista corregido y el álbum del `album_artist` (mapa por albumId); `deleteNotIn`/`clear` limpian artistas/álbumes huérfanos.

## Convenciones de trabajo

- **Commits por feature**, mensajes en español, imperativo (`feat:`, `fix:`, `refactor:`).
- Compilar siempre antes de commitear (`assembleRelease` verde).
- `PENDIENTES.md` es local (gitignore) y sirve de hoja de ruta: siguientes pasos Widgets → Casting (+ letras planas con auto-scroll opcional).

## Gotchas de arquitectura (no volver a caer)

- **Tab activo de Biblioteca**: guardado en `LibraryViewModel` como `StateFlow` (`selectTab`). Un `rememberSaveable` local se pierde al navegar al detalle y volver → reseteaba a "Canciones".
- **Grid de álbumes**: `BoxWithConstraints`; 2 columnas (<620dp), 3 (620–899), 4 (>=900dp). Los covers se vuelven gigantes si se fija `GridCells.Fixed(2)` en horizontal.
- **Header de detalle**: en `>=900dp` es una fila cover 180dp + título; en vertical es columna al 70% de ancho. Un cover demasiado grande empuja las canciones fuera de pantalla en landscape y parece que no hay resultados.
- Mantener el **patrón de colores adaptativos** en el reproductor: cambios al tema base se descartaron por preferencia del usuario.