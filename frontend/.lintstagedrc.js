export default {
  '**/*.{js,ts,css,json,md,yaml,yml}': ['prettier --write'],
  '**/*.{js,ts}': ['eslint --fix'],
};
