module.exports = {
  env: {
    browser: true,
    es2021: true,
    node: true,
  },
  extends: ['plugin:react/recommended', 'standard', 'prettier'],
  parser: '@typescript-eslint/parser',
  parserOptions: {
    ecmaFeatures: {
      jsx: false,
    },
    ecmaVersion: 12,
    sourceType: 'module',
  },
  plugins: ['react', '@typescript-eslint', 'prettier'],
  rules: {
    'no-undef': 'off',
    '@typescript-eslint/no-use-before-define': ['off'],
    'no-use-before-define': 'off',
    'space-before-function-paren': ['off'],
    'prettier/prettier': 'error',
    'no-unused-vars': ['off'],
    '@typescript-eslint/no-unused-vars': ['warn'],
    'no-useless-return': ['off'],
    'spaced-comment': ['off'],
    'react-hooks/exhaustive-deps': ['off'],
    'react/react-in-jsx-scope': 'off',
    'no-console': 'error',
  },
  settings: {
    react: {
      version: 'detect', // React version. "detect" automatically picks the version you have installed.
    },
  },
};
