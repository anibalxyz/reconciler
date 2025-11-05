// @ts-check

import { defineConfig } from 'eslint/config';
import baseConfig from '../eslint.config.mjs';
import eslintReact from '@eslint-react/eslint-plugin';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';

export default defineConfig(
  baseConfig,
  reactHooks.configs.flat.recommended,
  reactRefresh.configs.recommended,
  eslintReact.configs['recommended-typescript'],
  {
    name: 'dashboard-react',
    files: ['**/*.tsx', '**/*.jsx'],
  },
);
