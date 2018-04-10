ADB=~/Android/Sdk/platform-tools/adb
APK=./app/build/outputs/apk/debug/app-debug.apk

.PHONY: build

build:
	./gradlew build

deploy:
	$(ADB) install -r $(APK)

install: build deploy
	@echo "Done."

check:
	./gradlew test

clean:
	./gradlew clean

log:
	$(ADB) logcat |grep TCC

log-full:
	$(ADB) logcat
