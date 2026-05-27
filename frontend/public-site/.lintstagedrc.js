import baseConfig from '../.lintstagedrc.js';

export default {
  ...baseConfig,
  '**/*.astro': ['prettier --write', 'eslint --fix'],
};
