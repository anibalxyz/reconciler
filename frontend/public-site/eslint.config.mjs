// @ts-check

import baseConfig from '../eslint.config.mjs';
import { defineConfig } from 'eslint/config';
import astroeslint from 'eslint-plugin-astro';

export default defineConfig(
  {
    ignores: ['.astro/**'],
  },
  baseConfig,
  astroeslint.configs.recommended,
  {
    name: 'public-site-astro',
    files: ['**/*.astro'],
    plugins: {
      astro: astroeslint,
    },
  },
);
