
PWD=$(shell pwd)
AAP_JUCE_DIR=$(PWD)/external/aap-juce

APP_NAME=Odin2

APP_BUILD_DIR=$(PWD)
APP_SRC_DIR=$(PWD)/external/odin2
JUCE_DIR=$(PWD)/external/JUCE

APP_ICON=$(APP_SRC_DIR)/manual/graphics/logo.png

APP_SHARED_CODE_LIBS="$(APP_NAME)_artefacts/lib$(APP_NAME)_SharedCode.a lib${APP_NAME}_BinaryData.a"

PATCH_FILE=$(PWD)/aap-juce-support.patch
PATCH_DEPTH=1

JUCE_PATCHES= \
	$(shell pwd)/external/aap-juce/juce-patches/8.0.12/support-plugin-ui.patch \
	$(shell pwd)/external/aap-juce/juce-patches/8.0.12/juce-component-peer-view-touch.patch \
	$(shell pwd)/external/aap-juce/juce-patches/8.0.12/component-peer-view-lint.patch \
	$(shell pwd)/external/aap-juce/juce-patches/8.0.12/android-opengl-local-bounds.patch

include $(AAP_JUCE_DIR)/Makefile.cmake-common
