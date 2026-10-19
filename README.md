# Dreaming Client (Fabric 1.21.1, singleplayer only)

Modules: ESP, StorageESP, Nametags. Open the GUI with Right Shift.
Left-click a module to toggle, right-click to expand its settings, drag panel headers to move them.

Build:   gradle wrapper --gradle-version 8.10.2   (once, if there is no gradlew yet)
         ./gradlew build        -> build/libs/dreaming-client-0.1.0.jar
Run dev: ./gradlew runClient

Requires JDK 21. Install the jar in a 1.21.1 Fabric profile together with Fabric API.
All modules are hard-gated by DreamingClient.allowed() and do nothing outside singleplayer.
