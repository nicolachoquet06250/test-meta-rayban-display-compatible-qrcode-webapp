import { defineConfig } from 'vite';

// Assets relatifs : compatibles avec https://<owner>.github.io/<repo>/
// et avec un hébergement à la racine du domaine.
export default defineConfig({
  base: './',
});
