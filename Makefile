PACKAGE = de.markusfisch.android.pielauncher

all: debug install start

debug:
	./gradlew assembleDebug

lint:
	./gradlew lintDebug

findbugs:
	./gradlew findBugs

infer: clean
	infer -- ./gradlew assembleDebug

release: lint findbugs
	./gradlew assembleRelease

bundle: lint findbugs
	./gradlew bundleRelease

install:
	adb $(TARGET) install -r app/build/outputs/apk/debug/app-debug.apk

start:
	adb $(TARGET) shell 'am start -n \
		$(PACKAGE).debug/$(PACKAGE).activity.HomeActivity'

uninstall:
	adb $(TARGET) uninstall $(PACKAGE).debug

clean:
	./gradlew clean
