
#!/usr/bin/env bash
set -euo pipefail

# Configuration
ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
ANDROID_API="${ANDROID_API:-36}"
BUILD_TOOLS="${BUILD_TOOLS:-36.0.0}"

TOOLS_VERSION="15859902"
TOOLS_SHA256="4e4c464f145a7512b57d088ac6c278c03c9eea610886b35a5e0804e74eedf583"
TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-${TOOLS_VERSION}_latest.zip"

echo "=== Installation du SDK Android ==="

# Vérification de l'architecture
if [[ "$(uname -m)" != "x86_64" ]]; then
  echo "Erreur : ce script cible Linux x86_64."
  exit 1
fi

# 1. Dépendances
echo "[1/6] Installation des dépendances..."

sudo apt-get update
sudo apt-get install -y \
  openjdk-17-jdk \
  curl \
  unzip \
  ca-certificates

# 2. Téléchargement
echo "[2/6] Téléchargement des outils Android..."

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

curl -fL --retry 3 \
  "$TOOLS_URL" \
  -o "$TMP_DIR/cmdline-tools.zip"

echo "$TOOLS_SHA256  $TMP_DIR/cmdline-tools.zip" \
  | sha256sum -c -

# 3. Installation des outils CLI
echo "[3/6] Installation des Command-Line Tools..."

mkdir -p "$ANDROID_HOME/cmdline-tools"

unzip -q "$TMP_DIR/cmdline-tools.zip" -d "$TMP_DIR"

rm -rf "$ANDROID_HOME/cmdline-tools/latest"
mv "$TMP_DIR/cmdline-tools" \
   "$ANDROID_HOME/cmdline-tools/latest"

export ANDROID_HOME
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

# 4. Licences
echo "[4/6] Acceptation des licences Android..."

# yes retourne SIGPIPE si sdkmanager ferme stdin.
yes | sdkmanager --sdk_root="$ANDROID_HOME" --licenses \
  || [[ "${PIPESTATUS[1]}" -eq 0 ]]

# 5. Installation du SDK
echo "[5/6] Installation des composants SDK..."

sdkmanager --sdk_root="$ANDROID_HOME" --install \
  "platform-tools" \
  "platforms;android-35" \
  "platforms;android-${ANDROID_API}" \
  "build-tools;35.0.0" \
  "build-tools;${BUILD_TOOLS}"

# 6. Variables d'environnement
echo "[6/6] Configuration de l'environnement..."

ENV_FILE="$HOME/.android-sdk-env"

cat > "$ENV_FILE" <<'EOF'
export ANDROID_HOME="$HOME/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
EOF

# Prise en charge d'un chemin personnalisé
if [[ "$ANDROID_HOME" != "$HOME/Android/Sdk" ]]; then
  printf 'export ANDROID_HOME=%q\n' "$ANDROID_HOME" \
    > "$ENV_FILE"
  cat >> "$ENV_FILE" <<'EOF'
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
EOF
fi

for RC_FILE in "$HOME/.bashrc" "$HOME/.zshrc"; do
  if [[ -f "$RC_FILE" ]] && \
     ! grep -Fq '.android-sdk-env' "$RC_FILE"; then
    echo '[ -f "$HOME/.android-sdk-env" ] && source "$HOME/.android-sdk-env"' \
      >> "$RC_FILE"
  fi
done

# Vérification
echo ""
echo "=== Vérification ==="

java -version
sdkmanager --version
adb version

echo ""
echo "Installation terminée !"
echo "ANDROID_HOME=$ANDROID_HOME"
echo ""
echo "Active la configuration avec :"
echo "source ~/.android-sdk-env"
