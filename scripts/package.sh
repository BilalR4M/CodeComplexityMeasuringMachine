#!/usr/bin/env bash
# usage: scripts/package.sh <Windows|Linux|macOS> <numeric-version> <label>
# Run after `mvn verify` on the target OS. Output goes to dist/.
set -euo pipefail
OS="$1"; VER="$2"; LABEL="$3"
APP=ccmm-app
LIBS="$APP/target/libs"
JAR="$APP-$VER.jar"

rm -rf dist && mkdir -p dist
cp "$APP/target/$JAR" "$LIBS/"

# JVM flags are a starting point; tune from Phase 0 benchmarks.
COMMON=(--name CCMM-IDE --app-version "$VER" --vendor "Bilal Awshid" --description "CCMM IDE"
        --input "$LIBS" --main-jar "$JAR" --main-class dev.bilal.ccmm.app.Launcher
        --add-modules java.se,jdk.unsupported,jdk.crypto.ec
        --jlink-options "--strip-debug --no-header-files --no-man-pages"
        --java-options "-XX:+UseG1GC" --java-options "-Xshare:auto" --dest dist)

case "$OS" in
  Windows) TYPES=(msi)
    EXTRA=(--win-menu --win-shortcut --win-dir-chooser --win-upgrade-uuid a3f1c7e2-5b9d-4c68-9e21-7d4b0f6a8c13);;
  Linux) TYPES=(deb rpm)
    EXTRA=(--linux-shortcut --linux-menu-group Development --linux-package-name ccmm-ide --linux-app-category devel);;
  macOS) TYPES=(dmg)
    EXTRA=(--mac-package-name CCMM-IDE --mac-package-identifier dev.bilal.ccmm.ide)
    if [[ -n "${MAC_SIGN_IDENTITY:-}" ]]; then EXTRA+=(--mac-sign --mac-signing-key-user-name "$MAC_SIGN_IDENTITY"); fi;;
  *) echo "unknown OS: $OS"; exit 2;;
esac

for T in "${TYPES[@]}"; do jpackage --type "$T" "${COMMON[@]}" "${EXTRA[@]}"; done

# Optional signing (inert unless secrets are provided)
if [[ "$OS" == "Windows" && -n "${WIN_CERT_FILE:-}" ]]; then
  SIGNTOOL="$(ls "/c/Program Files (x86)/Windows Kits/10/bin/"*/x64/signtool.exe | sort | tail -1)"
  "$SIGNTOOL" sign /f "$WIN_CERT_FILE" /p "$WIN_CERT_PASSWORD" /fd sha256 /tr http://timestamp.digicert.com /td sha256 dist/*.msi
fi
if [[ "$OS" == "macOS" && -n "${APPLE_ID:-}" && -n "${MAC_SIGN_IDENTITY:-}" ]]; then
  # Validate hardened-runtime/entitlements on the first signed run.
  xcrun notarytool submit dist/*.dmg --apple-id "$APPLE_ID" --team-id "$APPLE_TEAM_ID" --password "$APPLE_APP_PASSWORD" --wait
  xcrun stapler staple dist/*.dmg
fi

# Disambiguate per-arch names (msi/dmg carry no arch); deb/rpm already do.
for f in dist/*.msi dist/*.dmg; do
  [[ -e "$f" ]] || continue
  mv "$f" "${f%.*}-$LABEL.${f##*.}"
done

# Ship the CLI once, from the Linux build.
if [[ "$OS" == "Linux" ]]; then
  cp ccmm-cli/target/ccmm-cli-"$VER"-all.jar "dist/ccmm-cli-$VER.jar"
fi

cd dist
if command -v sha256sum >/dev/null; then sha256sum -- * > "SHA256SUMS-$LABEL.txt"; else shasum -a 256 -- * > "SHA256SUMS-$LABEL.txt"; fi
