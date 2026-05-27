import baseConfig from '../.lintstagedrc.js';

export default {
  ...baseConfig,
  '**/*.{jsx,tsx}': ['prettier --write', 'eslint --fix'],
};
