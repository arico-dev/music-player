# music-player

Reproductor de música para Android, **local y sin anuncios**, pensado para tu biblioteca personal en el dispositivo.

Hecho a medida con Kotlin, Jetpack Compose y Media3 (ExoPlayer), con una UI Material 3 que usa color dinámico (Material You).

## Qué hace

- Escanea tu almacenamiento y monta una biblioteca a partir de MediaStore.
- Reproduce tus canciones con cola completa: siguiente/anterior, aleatorio, repetir (todo / una), barra de progreso con seek y selección directa de canción en la cola.
- Now Playing a pantalla completa que extrae el color dominante del artwork para teñir el fondo.
- Navegación entre Inicio / Biblioteca / Buscar / Ajustes con un mini-player fijo sobre la barra inferior (carátula, play/pausa, siguiente — tap para abrir el reproductor).
- Pedido de permisos de audio en el primer arranque.

## Estado actual

El reproductor base ya funciona (biblioteca, reproducción, cola y mini-player). La ruta por delante está definida en `PENDIENTES.md`: biblioteca completa (álbumes/artistas/géneros), búsqueda full-text, letras sincronizadas, playlists, widgets y casting.

## Arquitectura

Un solo módulo `:app` con clean architecture por feature:

- `core/` — modelos de dominio (Song, Album, Artist, Genre), tema, utilidades.
- `data/` — escaneo MediaStore, Room local, red (LRCLIB/Deezer, por implementar) y repositorio.
- `player/` — `PlaybackController`, singleton Media3 que expone StateFlows consumidos por los ViewModels.
- `feature/<nombre>/` — una pantalla + su ViewModel por feature.
- `ui/theme/` — tema Material 3 con color dinámico.
