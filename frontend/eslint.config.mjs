// @ts-check

import eslint from '@eslint/js';
import { defineConfig } from 'eslint/config';
import tsEslint from 'typescript-eslint';
import prettierEslint from 'eslint-config-prettier/flat';

export default defineConfig(eslint.configs.recommended, tsEslint.configs.recommended, prettierEslint, {
  name: 'frontend-base',
  ignores: ['**/dist/**', '**/node_modules/**'],
  files: ['**/*.ts', '**/*.js'],
});
