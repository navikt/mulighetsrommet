// eslint.config.mjs
import tseslint from 'typescript-eslint';
import config from '@mr/eslint-config-react-app';

export default [
  {
    files: ['**/*.{ts,tsx}'],
    languageOptions: {
      parser: tseslint.parser,
      parserOptions: {
        projectService: true,               // automatically finds tsconfig
        tsconfigRootDir: import.meta.dirname,
        // Or, instead of projectService:
        // project: ['./tsconfig.json'],
      },
    },
    plugins: {
      '@typescript-eslint': tseslint.plugin,
    },
    rules: {
      '@typescript-eslint/switch-exhaustiveness-check': 'error',
    },
  },

  ...config,

  {
    ignores: ['dist', 'playwright-report'],
  },
];
