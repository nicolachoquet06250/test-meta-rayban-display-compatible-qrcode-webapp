# QR Lens — Meta Ray-Ban Display Web App

Web App statique de lecture de contenus QR transmis depuis une source externe. **Cette application ne peut pas accéder à la caméra des lunettes et ne scanne pas physiquement un QR code.**

## Exécution

```bash
npm install
npm run test
npm run dev
npm run build
```

Servir `dist/` en HTTPS pour les lunettes (Vercel, Netlify, nginx, etc.). Le manifeste doit rester accessible à `/.well-known/meta-wearables-manifest.json`, et l'icône à `/icons/qr-lens.svg`. Pour un déploiement dans un sous-répertoire, adapter le `base` Vite et le chemin vers les assets ; la distribution par défaut cible la racine d'un domaine.

## Données externes

Le système qui décode le QR code ouvre :

`https://votre-domaine.example/?data=<contenu-encode-avec-encodeURIComponent>`

Exemple JavaScript côté source :

```js
const qrContent = 'WIFI:T:WPA;S:Home;P:secret;;';
const url = `https://votre-domaine.example/?data=${encodeURIComponent(qrContent)}`;
// Ouvrir l'URL via le mécanisme de lancement autorisé par le dispositif source.
```

Cette technique n'offre **pas** de push en direct vers une application déjà ouverte, ni de synchronisation entre appareils. Pour cette capacité, prévoir un canal supplémentaire (WebSocket et un serveur de relais, par exemple).

Les données ne sont pas envoyées à un serveur par le code JavaScript de l'application, mais le paramètre `data` figure dans **la requête HTTP initiale**, et peut apparaître dans les journaux du serveur d'hébergement. Éviter les mots de passe et autres données sensibles via cette méthode. Le paramètre est retiré de l'historique visible après le chargement.

## Tests

Utiliser le simulateur Meta Ray-Ban Display dans Chrome pour vérifier le rendu à 600×600 et la navigation directionnelle, puis tester sur les lunettes réelles. Le projet n'a pas fait l'objet de validation matérielle.

## Restrictions

- Seules les URLs HTTPS peuvent être ouvertes directement via le bouton dédié.
- Le mot de passe Wi-Fi est masqué dans le résultat, mais conservé dans la zone de saisie pour permettre la modification ; ne pas traiter des secrets avec l'intégration URL.
- Les contacts vCard sont uniquement affichés, non importés automatiquement.
- Le manifeste utilise une icône SVG monochrome ; vérifier sa compatibilité matérielle. Le générateur officiel d'icônes Meta permet de produire une version WebP pour une prise en charge optimale.

## GitHub Actions / GitHub Pages

Le workflow `.github/workflows/deploy.yml` :

1. Lance **tous les tests** (`npm test`) sur chaque push vers `main`, chaque PR ciblant `main`, ou à la demande.
2. Si les tests réussissent, et uniquement sur `main` (ou lancement manuel depuis `main`), installe les dépendances et compile l'application (`npm run build`).
3. Publie **le contenu de `dist/`**, et non son répertoire parent, à la **racine de `gh-pages`**. Le répertoire `.well-known/` est conservé, avec un fichier `.nojekyll` ajouté par l'action de publication. La branche `gh-pages` est créée automatiquement si elle n'existe pas.

### Réglages GitHub nécessaires

- `Settings > Actions > General > Workflow permissions` : autoriser l'écriture pour `GITHUB_TOKEN` (si la politique du dépôt le permet). Le job de déploiement demande explicitement `contents: write`.
- `Settings > Pages > Build and deployment > Source` : choisir **Deploy from a branch**, puis `gh-pages` et `/(root)`.
- Si la branche principale du dépôt n'est pas `main`, adapter `branches` et la condition `github.ref` dans le workflow.

Le fichier `vite.config.js` configure `base: './'` afin que les ressources restent accessibles aussi bien depuis `https://<owner>.github.io/<repo>/` qu'à la racine d'un domaine. Pour l'intégration externe : utiliser **l'URL exacte du déploiement**, avec son éventuel segment `/<repo>/` avant `?data=...`.

Attention : `force_orphan: true` recrée à chaque publication l'historique de `gh-pages`. Cette branche est destinée exclusivement aux artefacts générés. Aucun push GitHub n'a été effectué par cette livraison.
