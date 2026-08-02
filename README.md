# Redline

App Android nativo (Kotlin + Jetpack Compose) para rastrear viagens via GPS,
focado no público gearhead. MVP com armazenamento 100% local (Room/SQLite), sem
backend.

## Stack
- Kotlin + Jetpack Compose (Material 3)
- Room (SQLite) — persistência local
- FusedLocationProviderClient (Google Play Services) — GPS de alta precisão
- Foreground Service tipo `location` — tracking com a tela desligada
- MVVM (ViewModel + Repository) + Coroutines/Flow

## Como rodar
1. Abra a pasta no **Android Studio** (Ladybug ou mais recente). No primeiro
   *Gradle Sync* a IDE baixa o Gradle 8.11.1 definido no wrapper e resolve as
   dependências.
2. **Chave do Google Maps** (necessária só para o mapa na tela de detalhe):
   crie um arquivo `secrets.properties` na raiz do projeto com:
   ```
   MAPS_API_KEY=SUA_CHAVE_DO_MAPS_SDK_ANDROID
   ```
   Sem a chave o app compila e roda normalmente; apenas o mapa aparece em branco
   (veja `secrets.defaults.properties`).
3. Rode em um dispositivo físico (o emulador dá pontos de GPS simulados). Conceda
   a permissão de localização e, para tracking ininterrupto, "Permitir o tempo
   todo" (background).

> Build por linha de comando: `./gradlew assembleDebug` (requer o Android SDK e a
> variável `ANDROID_HOME`/`sdk.dir` configurados — o Android Studio faz isso).

## Arquitetura

```
com.gearhead.redline
├── RedlineApp / MainActivity            App + host do Compose
├── di/ServiceLocator                    DI manual (1 repositório; troque por Hilt se crescer)
├── data
│   ├── local/entity                     TripEntity, LocationPointEntity, TripWithPoints (1:N)
│   ├── local/RedlineDatabase, TripDao   Room
│   └── repository/TripRepository        único ponto de acesso à persistência
├── location                             NÚCLEO DA GRAVAÇÃO
│   ├── LocationTrackingService          foreground service: dono do ciclo de vida da viagem
│   ├── GpsSample / GpsFilter            filtro de ruído (accuracy, saltos, jitter parado)
│   ├── TripMetricsAccumulator           distância (Haversine), top/avg speed, tempo em movimento
│   ├── GeoMath                          Haversine
│   ├── LiveTripState / RecordingState   ponte service → UI (StateFlow singleton)
│   └── TrackingConfig                   tunables (intervalos, thresholds)
├── ui
│   ├── theme                            tema escuro "cockpit" (âmbar/vermelho, mono p/ números)
│   ├── record                           tela de gravação (cockpit) + ViewModel
│   ├── history                          lista de viagens + ViewModel
│   ├── detail                           detalhe com mapa/polyline + ViewModel
│   ├── permissions                      fluxo runtime fine + background + notificações
│   └── navigation/RedlineNavHost        rotas: record → history → trip/{id}
├── util/Formatters                      m/s→km/h, m→km, duração, datas
└── export                               TripExporter (GPX/CSV) — PREPARADO, fora do MVP
```

### Fluxo de gravação (feature principal)
1. `RecordScreen` valida a permissão e chama `LocationTrackingService.start()`.
2. O service cria a linha `Trip` (início), sobe como foreground (notificação
   persistente) e pede updates de localização a ~1 Hz com `PRIORITY_HIGH_ACCURACY`.
3. Cada fix passa pelo `GpsFilter` (descarta baixa precisão, saltos implausíveis e
   jitter parado). Fixes aceitos alimentam o `TripMetricsAccumulator` e são
   gravados como `LocationPoint`.
4. O estado ao vivo é publicado em `RecordingState` (StateFlow) e refletido no
   cockpit e na notificação (velocidade + tempo + distância) a cada segundo.
5. Em `stop()`, o service consolida as métricas na linha `Trip` e encerra.

### Unidades
O app persiste em SI (m/s, metros, ms) e converte para km/h / km só na borda
(`Formatters`). Trocar para milhas é um único ponto de mudança.

## Testes
Testes unitários JVM da lógica sensível (sem device):
`app/src/test/.../location/` — `GeoMathTest`, `GpsFilterTest`,
`TripMetricsAccumulatorTest`. Rode com `./gradlew test`.

## Fora de escopo (não implementado)
Sync/nuvem, login, comparação/rankings entre viagens, exportação GPX/CSV
(interface `TripExporter` deixada preparada).
