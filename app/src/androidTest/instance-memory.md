# Odin2 instance memory regression

The AAP wrapper owned its JUCE processor but never deleted it on release. This
retained Odin2's DSP buffers and processor callbacks for every destroyed instance.
Release now deletes any active editor and then the processor on the JUCE message
thread, and erases both state and preset extension records.

Odin2 also allocated 160 × 33 × 512 floats (10,813,440 bytes, plus pointer arrays)
for unused generated factory wavetables in every instance. Playback already uses
compiled constant tables. That allocation and its unused loading path are removed;
editable chipdraw, wavedraw and specdraw tables remain private to each instance.

`InstanceLifetimeTest` exercises the actual AAP service/factory through Binder,
with the required extension shared memory. It checks 48 simultaneous instances
and 100 sequential create/destroy cycles in one process, asserting less than
8 MiB of native heap growth between cycles 10 and 100. It does not exercise audio
rendering or editor attachment.

Run against the chosen tablet:

```sh
ANDROID_SERIAL='<Samsung adb serial>' ./gradlew :app:connectedDebugAndroidTest -Pandroid.injected.build.abi=arm64-v8a
```

Verified on the connected Samsung SM-X230 on 2026-10-02 with arm64 native Release
code packaged in a debug APK. Native allocated heap (not resident memory) in the
previous build reached 721,208,064 bytes after 10 releases and 8,111,930,992 bytes
after 100; the regression assertion failed with 7,390,722,928 bytes of growth.

The fixed build passed both tests. After 48 live instances, native allocated heap
fell from 2,895,749,568 to 10,368,304 bytes when all were released. In the sequential
test it remained flat: 27,748,528 bytes after 10 releases and 27,745,696 after 100
(a decrease of 2,832 bytes). The fixed APK was left installed on the tablet.
