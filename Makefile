PACKAGE = de.markusfisch.android.pielauncher

all: debug install start

debug:
	./gradlew assembleDebug

lint:
	./gradlew lintDebug

findbugs:
	./gradlew findBugs

sonarqube:
	./gradlew sonarqube

infer: clean
	infer -- ./gradlew assembleDebug

release:
	@./gradlew \
		assembleRelease \
		-Pandroid.injected.signing.store.file=$(ANDROID_KEYFILE) \
		-Pandroid.injected.signing.store.password=$(ANDROID_STORE_PASSWORD) \
		-Pandroid.injected.signing.key.alias=$(ANDROID_KEY_ALIAS) \
		-Pandroid.injected.signing.key.password=$(ANDROID_KEY_PASSWORD)

install:
	adb $(TARGET) install -r app/build/outputs/apk/debug/app-debug.apk

start:
	adb $(TARGET) shell 'am start -n \
		$(PACKAGE).debug/$(PACKAGE).activity.HomeActivity'

uninstall:
	adb $(TARGET) uninstall $(PACKAGE).debug

clean:
	./gradlew clean
