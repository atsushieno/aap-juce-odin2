
PWD=$(shell pwd)
AAP_JUCE_DIR=$(PWD)/external/aap-juce

APP_NAME=Odin2

APP_BUILD_DIR=$(PWD)
APP_SRC_DIR=$(PWD)/external/odin2
JUCE_DIR=$(APP_SRC_DIR)/libs/JUCE

APP_SHARED_CODE_LIBS="$(APP_NAME)_artefacts/lib$(APP_NAME)_SharedCode.a lib${APP_NAME}_BinaryData.a"

PATCH_FILE=$(PWD)/aap-juce-support.patch
PATCH_DEPTH=1

include $(AAP_JUCE_DIR)/Makefile.cmake-common
