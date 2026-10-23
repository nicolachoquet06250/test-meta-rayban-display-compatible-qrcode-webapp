# Meta Ray-Ban Display · QR Code

Projet réunissant deux expériences complémentaires pour les **Meta Ray-Ban Display** : une **Web App** destinée au navigateur des lunettes et une **application Android native** qui accède à la caméra des lunettes pour scanner les QR codes.

> **Important :** les Web Apps Meta Ray-Ban Display n'ont pas accès au flux de la caméra des lunettes. Le scan vidéo est donc réalisé par l'application Android au moyen du **Meta Wearables Device Access Toolkit (DAT)**. Les deux applications ne partagent pas automatiquement une session ni les résultats : toute synchronisation Web ↔ Android nécessite un mécanisme explicite.

## Architecture

```text
Meta Ray-Ban Display
├── Navigateur intégré → Web App (interface web)
└── Caméra → SDK Meta Wearables DAT → Application Android
                                      └── Trames YUV → ML Kit → QR décodé
```

### Application Web

La Web App s'exécute dans le navigateur intégré aux lunettes et doit être servie via **HTTPS**. L'installation sur les lunettes s'effectue avec un lien d'installation Meta (ou son QR code), et non avec un simple QR de l'URL du site.

Schéma de lien :

```text
fb-viewapp://web_app_deep_link?appName=<nom>&appUrl=<url-https>
```

La Web App ne peut **ni accéder à la caméra des lunettes ni décoder directement ses images**. Pour une fonctionnalité de scan depuis cette caméra, utiliser la partie Android.

> Le code source Web n'était pas inclus dans l'archive Android examinée lors de la rédaction. Ses commandes d'installation, scripts, dossiers, frameworks et fonctionnalités détaillées doivent être complétés après vérification des sources Web réelles ; aucune commande spécifique à un framework n'est supposée ici.

### Application Android

Le module `app/` est une application Android Kotlin. Il :

1. Initialise `Wearables` ;
2. Propose l'enregistrement auprès de Meta AI et la demande d'autorisation `Permission.CAMERA` ;
3. Crée une session avec `AutoDeviceSelector` ;
4. Attache la caméra lorsque la session devient `DeviceSessionState.STARTED` ;
5. Reçoit les trames vidéo à 15 FPS (`VideoQuality.MEDIUM`, vidéo non compressée) ;
6. Analyse la luminance des images via **Google ML Kit Barcode Scanning** ;
7. Affiche le dernier QR détecté et permet de copier son contenu.

Un flux décodé nécessite une autorisation caméra accordée dans Meta AI, une connexion active aux lunettes et une session DAT valide.

## Prérequis

**Android**
- JDK 17
- Android SDK plateforme 35 et Build Tools 35
- Gradle 8.11.1 et Android Gradle Plugin 8.9.2
- SDK `com.meta.wearable:mwdat-core:1.0.0` et `mwdat-camera:1.0.0`
- Téléphone Android (minSdk 29) compatible avec Meta Wearables DAT
- Application Meta AI et lunettes compatibles, appairées
- Mode développeur Meta Wearables ou identifiants d'application obtenus via le Developer Center

**Web**
- Environnement dépendant des sources Web effectives
- Hébergement HTTPS accessible depuis les lunettes
- Meta Ray-Ban Display pour la validation sur matériel

## Construction Android

À la racine du dépôt :

```bash
./gradlew testDebugUnitTest assembleDebug
```

Sur Windows :

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

APK attendu : `app/build/outputs/apk/debug/app-debug.apk`.

### Identifiants Meta

Les manifest placeholders `mwdat_application_id` et `mwdat_client_token` sont configurés à `0` par défaut pour **Developer Mode**. Hors Developer Mode, transmettre les identifiants fournis par le Developer Center via des propriétés Gradle sécurisées :

```bash
./gradlew assembleDebug -Pmwdat_application_id=VOTRE_ID -Pmwdat_client_token=VOTRE_TOKEN
```

Ne pas committer de secrets ou tokens réels. Le callback d'enregistrement est déclaré dans `app/src/main/AndroidManifest.xml`.

### Wrapper Gradle

Les fichiers `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties` et **le JAR officiel** `gradle/wrapper/gradle-wrapper.jar` doivent normalement être commités ensemble.

Le workflow CI inclus dans l'archive Android génère le wrapper officiel Gradle 8.11.1 avant de lancer les tâches ; il n'utilise pas le bootstrap Java personnalisé initial, rejeté par la validation de GitHub Actions. La présence d'un wrapper généré pendant la CI ne dispense pas de committer le wrapper officiel pour permettre un build local immédiat.

## GitHub Actions

Workflow : `.github/workflows/android-apk.yml`.

Déclencheurs : `push`, `pull_request`, `workflow_dispatch`.

Étapes : JDK 17, Gradle 8.11.1, installation SDK Android 35, tests `testDebugUnitTest`, build `assembleDebug`, publication de l'artefact `meta-rayban-qr-scanner-debug-apk`.

L'artefact APK se récupère dans **GitHub → Actions → exécution du workflow → Artifacts**. Le pipeline ne déploie pas automatiquement sur les lunettes.

## Arborescence Android confirmée

```text
.github/workflows/android-apk.yml
app/
  build.gradle.kts
  src/main/
    AndroidManifest.xml
    java/com/example/metaqr/
      MainActivity.kt
      QrFrameDecoder.kt
  src/test/java/com/example/metaqr/
    FrameLayoutTest.kt
build.gradle.kts
settings.gradle.kts
gradle.properties
gradle/wrapper/gradle-wrapper.properties
```

`MainActivity.kt` : enregistrement, permissions, session DAT, caméra et UI Android.

`QrFrameDecoder.kt` : extraction luminance d'une trame YUV et décodage QR avec ML Kit.

`FrameLayoutTest.kt` : test unitaire de la manipulation des buffers vidéo.

L'emplacement réel de la Web App doit être documenté après accès à son code source.

## Tests et limites de validation

La suite Android se lance avec :

```bash
./gradlew testDebugUnitTest
```

La compilation Android en CI doit réussir avant de considérer le binaire comme validé. La correction de l'import `DeviceSessionState` (package `com.meta.wearable.dat.core.session`) résout l'erreur de symbole remontée, mais ne constitue pas une preuve de compilation complète.

À vérifier sur appareils réels : appairage, demande de permission, démarrage/arrêt caméra, décodage de différents QR codes, comportement des interruptions et affichage éventuel des résultats dans la Web App.

**Limitations connues :**
- La Web App ne peut pas scanner directement via la caméra ;
- L'intégration de l'affichage natif dans les lunettes via `mwdat-display` n'est pas implémentée dans le module Android examiné : les résultats sont affichés sur le téléphone ;
- Les deux applications ne disposent d'aucune synchronisation documentée ;
- La disposition exacte et les strides YUV doivent être validés avec les trames DAT réelles ;
- Il n'y a aucune preuve de test de bout en bout sur lunettes physiques.

## Documentation

- [Meta Wearables DAT — intégration Android](https://wearables.developer.meta.com/docs/develop/dat/build-integration-android)
- [Meta Wearables DAT — streaming caméra](https://wearables.developer.meta.com/docs/develop/dat/streaming)
- [Meta Wearables — Web Apps](https://wearables.developer.meta.com/docs/develop/webapps/)
- [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html)
