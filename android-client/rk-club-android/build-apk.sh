#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DIST_DIR="${ROOT_DIR}/dist"
FINAL_APK="${DIST_DIR}/rk-club-debug.apk"
STAGE_DIR="$(mktemp -d "${TMPDIR:-/tmp}/rkclub-android-build.XXXXXX")"

ANDROID_SDK="${ANDROID_SDK:-${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}}"
JAVA_HOME_ARG="${JAVA_HOME:-}"
VERSION_NAME="0.0.43"
VERSION_CODE="44"
ANDROID_PLATFORM="${ANDROID_PLATFORM:-android-36}"
BUILD_TOOLS_VERSION="${ANDROID_BUILD_TOOLS_VERSION:-36.0.0}"
ANDROID_TOOLS_URL="${ANDROID_TOOLS_URL:-https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip}"
ANDROID_TOOLS_TIMEOUT_SECONDS="${ANDROID_TOOLS_TIMEOUT_SECONDS:-180}"

while [ "$#" -gt 0 ]; do
  case "$1" in
    --android-sdk|--androidSdk|-AndroidSdk)
      ANDROID_SDK="${2:-}"
      shift 2
      ;;
    --java-home|--javaHome|-JavaHome)
      JAVA_HOME_ARG="${2:-}"
      shift 2
      ;;
    --version-name|--versionName|-VersionName)
      VERSION_NAME="${2:-}"
      shift 2
      ;;
    --version-code|--versionCode|-VersionCode)
      VERSION_CODE="${2:-}"
      shift 2
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 2
      ;;
  esac
done

cleanup() {
  rm -rf "${STAGE_DIR}"
}
trap cleanup EXIT

resolve_tool() {
  local name="$1"
  local preferred="${2:-}"
  if [ -n "${preferred}" ] && [ -x "${preferred}" ]; then
    printf '%s\n' "${preferred}"
    return 0
  fi
  if command -v "${name}" >/dev/null 2>&1; then
    command -v "${name}"
    return 0
  fi
  echo "Required tool not found: ${name}" >&2
  exit 1
}

ensure_android_sdk() {
  if [ -z "${ANDROID_SDK}" ]; then
    ANDROID_SDK="${ROOT_DIR}/.android-sdk"
  fi
  mkdir -p "${ANDROID_SDK}"
  export ANDROID_HOME="${ANDROID_SDK}"
  export ANDROID_SDK_ROOT="${ANDROID_SDK}"

  local sdkmanager="${ANDROID_SDK}/cmdline-tools/latest/bin/sdkmanager"
  if [ ! -x "${sdkmanager}" ]; then
    echo "Downloading Android command line tools"
    local tools_zip="${STAGE_DIR}/cmdline-tools.zip"
    if ! curl -fL --retry 2 --connect-timeout 20 --max-time "${ANDROID_TOOLS_TIMEOUT_SECONDS}" -o "${tools_zip}" "${ANDROID_TOOLS_URL}"; then
      echo "Android command line tools download failed: ${ANDROID_TOOLS_URL}" >&2
      exit 1
    fi
    if [ ! -s "${tools_zip}" ]; then
      echo "Android command line tools download is empty: ${ANDROID_TOOLS_URL}" >&2
      exit 1
    fi
    mkdir -p "${STAGE_DIR}/cmdline-tools-unpack" "${ANDROID_SDK}/cmdline-tools"
    (cd "${STAGE_DIR}/cmdline-tools-unpack" && jar xf "${tools_zip}")
    rm -rf "${ANDROID_SDK}/cmdline-tools/latest"
    mv "${STAGE_DIR}/cmdline-tools-unpack/cmdline-tools" "${ANDROID_SDK}/cmdline-tools/latest"
    chmod +x "${sdkmanager}" || true
  fi

  export PATH="${ANDROID_SDK}/cmdline-tools/latest/bin:${ANDROID_SDK}/platform-tools:${PATH}"
  if [ ! -f "${ANDROID_SDK}/platforms/${ANDROID_PLATFORM}/android.jar" ] ||
     [ ! -d "${ANDROID_SDK}/build-tools/${BUILD_TOOLS_VERSION}" ]; then
    echo "Installing Android SDK ${ANDROID_PLATFORM} and build-tools ${BUILD_TOOLS_VERSION}"
    yes | "${sdkmanager}" --sdk_root="${ANDROID_SDK}" --licenses >/tmp/rk-android-sdk-licenses.log 2>&1 || true
    "${sdkmanager}" --sdk_root="${ANDROID_SDK}" \
      "platforms;${ANDROID_PLATFORM}" \
      "build-tools;${BUILD_TOOLS_VERSION}" \
      "platform-tools"
  fi
}

if [ -n "${JAVA_HOME_ARG}" ]; then
  export JAVA_HOME="${JAVA_HOME_ARG}"
  export PATH="${JAVA_HOME}/bin:${PATH}"
fi

JAVAC="$(resolve_tool javac "${JAVA_HOME:-}/bin/javac")"
JAR="$(resolve_tool jar "${JAVA_HOME:-}/bin/jar")"

ensure_android_sdk

ANDROID_JAR="${ANDROID_SDK}/platforms/${ANDROID_PLATFORM}/android.jar"
BUILD_TOOLS_DIR="${ANDROID_SDK}/build-tools/${BUILD_TOOLS_VERSION}"
AAPT2="$(resolve_tool aapt2 "${BUILD_TOOLS_DIR}/aapt2")"
D8="$(resolve_tool d8 "${BUILD_TOOLS_DIR}/d8")"
ZIPALIGN="$(resolve_tool zipalign "${BUILD_TOOLS_DIR}/zipalign")"
APKSIGNER="$(resolve_tool apksigner "${BUILD_TOOLS_DIR}/apksigner")"

mkdir -p "${DIST_DIR}"
cp -a "${ROOT_DIR}/app" "${STAGE_DIR}/app"

APP_DIR="${STAGE_DIR}/app"
BUILD_DIR="${STAGE_DIR}/build"
STAGE_DIST_DIR="${STAGE_DIR}/dist"
FLAT_DIR="${BUILD_DIR}/compiled-res"
GEN_DIR="${BUILD_DIR}/generated"
CLASSES_DIR="${BUILD_DIR}/classes"
DEX_DIR="${BUILD_DIR}/dex"
UNSIGNED_APK="${BUILD_DIR}/rk-club-unsigned.apk"
ALIGNED_APK="${BUILD_DIR}/rk-club-aligned.apk"
STAGE_APK="${STAGE_DIST_DIR}/rk-club-debug.apk"
KEYSTORE="${ROOT_DIR}/signing/rkclub-release.keystore"

if [ ! -f "${KEYSTORE}" ]; then
  echo "Stable signing keystore not found: ${KEYSTORE}" >&2
  exit 1
fi

mkdir -p "${BUILD_DIR}" "${STAGE_DIST_DIR}" "${FLAT_DIR}" "${GEN_DIR}" "${CLASSES_DIR}" "${DEX_DIR}"

"${AAPT2}" compile --dir "${APP_DIR}/src/main/res" -o "${FLAT_DIR}"
mapfile -t FLAT_FILES < <(find "${FLAT_DIR}" -type f -name '*.flat' | sort)
"${AAPT2}" link \
  -I "${ANDROID_JAR}" \
  --manifest "${APP_DIR}/src/main/AndroidManifest.xml" \
  --java "${GEN_DIR}" \
  --min-sdk-version 23 \
  --target-sdk-version 36 \
  --version-name "${VERSION_NAME}" \
  --version-code "${VERSION_CODE}" \
  -o "${UNSIGNED_APK}" \
  "${FLAT_FILES[@]}"

find "${APP_DIR}/src/main/java" "${GEN_DIR}" -type f -name '*.java' | sort > "${BUILD_DIR}/sources.txt"
"${JAVAC}" -encoding UTF-8 -source 8 -target 8 -bootclasspath "${ANDROID_JAR}" -d "${CLASSES_DIR}" @"${BUILD_DIR}/sources.txt"

find "${CLASSES_DIR}" -type f -name '*.class' | sort > "${BUILD_DIR}/class-files.txt"
"${D8}" --lib "${ANDROID_JAR}" --min-api 23 --output "${DEX_DIR}" @"${BUILD_DIR}/class-files.txt"
"${JAR}" uf "${UNSIGNED_APK}" -C "${DEX_DIR}" classes.dex
"${ZIPALIGN}" -f -p 4 "${UNSIGNED_APK}" "${ALIGNED_APK}"

"${APKSIGNER}" sign \
  --ks "${KEYSTORE}" \
  --ks-key-alias rkclub \
  --ks-pass pass:rkclub2026 \
  --key-pass pass:rkclub2026 \
  --out "${STAGE_APK}" \
  "${ALIGNED_APK}"
"${APKSIGNER}" verify "${STAGE_APK}"

cp -f "${STAGE_APK}" "${FINAL_APK}"
echo "APK built: ${FINAL_APK}"
echo "Version: ${VERSION_NAME} (${VERSION_CODE})"
