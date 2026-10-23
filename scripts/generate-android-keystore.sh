#!/usr/bin/env bash
set -euo pipefail

KEYSTORE_FILE="metaqr-release.jks"
KEY_ALIAS="metaqr"
VALIDITY_DAYS=10000

if ! command -v keytool >/dev/null 2>&1; then
    echo "Erreur : keytool introuvable. Installe un JDK 17+."
    exit 1
fi

if [[ -f "$KEYSTORE_FILE" ]]; then
    echo "Erreur : $KEYSTORE_FILE existe déjà."
    echo "Pour conserver ta signature Android, ne remplace jamais ce fichier."
    exit 1
fi

echo "=== Génération du keystore Android ==="

keytool -genkeypair \
    -v \
    -keystore "$KEYSTORE_FILE" \
    -alias "$KEY_ALIAS" \
    -keyalg RSA \
    -keysize 4096 \
    -validity "$VALIDITY_DAYS" \
    -dname "CN=Meta QR Scanner, OU=Android, O=Meta QR Scanner, L=Paris, C=FR"

echo
echo "=== Empreintes du certificat ==="

keytool -list -v \
    -keystore "$KEYSTORE_FILE" \
    -alias "$KEY_ALIAS"

echo
echo "=== Encodage Base64 pour GitHub Actions ==="

if base64 --help 2>&1 | grep -q -- '-w'; then
    base64 -w 0 "$KEYSTORE_FILE" > "${KEYSTORE_FILE}.base64"
else
    base64 < "$KEYSTORE_FILE" | tr -d '\n' > "${KEYSTORE_FILE}.base64"
fi

echo
echo "Keystore : $KEYSTORE_FILE"
echo "Base64   : ${KEYSTORE_FILE}.base64"
echo
echo "Secrets GitHub Actions à configurer :"
echo "  ANDROID_KEYSTORE_BASE64  → contenu du fichier .base64"
echo "  ANDROID_KEYSTORE_PASSWORD → mot de passe du keystore"
echo "  ANDROID_KEY_ALIAS         → $KEY_ALIAS"
echo "  ANDROID_KEY_PASSWORD      → mot de passe de la clé"
echo
echo "IMPORTANT : sauvegarde le .jks et ses mots de passe."
echo "Ne les ajoute jamais à Git."
