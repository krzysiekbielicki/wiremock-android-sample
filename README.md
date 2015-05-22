Running tests
-------------

WireMock should be up and listening on 8080.
Try to use fixed version of WireMock [https://github.com/krzysiekbielicki/wiremock]
```
emulator-x86 -http-proxy http://127.0.0.1:8080 @Nexus_4_API_22 &
./gradlew connectedAndroidTestDebug
```