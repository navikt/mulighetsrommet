module.exports = {
  root: true,
  env: {
    browser: true,
    es2022: true,
    node: true,
  },
  extends: [
    "eslint:recommended",
    // TODO: utvid linting med dette
    // "plugin:@typescript-eslint/recommended",
    "plugin:react/recommended",
    "plugin:react-hooks/recommended",
    "prettier",
  ],
  ignorePatterns: ["dist", ".eslintrc.cjs"],
  parser: "@typescript-eslint/parser",
  plugins: ["@typescript-eslint", "react-refresh", "prettier"],
  rules: {
    "no-undef": "off",
    "@typescript-eslint/no-use-before-define": ["off"],
    "no-use-before-define": "off",
    "space-before-function-paren": ["off"],
    "prettier/prettier": "error",
    "no-unused-vars": ["off"],
    "@typescript-eslint/no-unused-vars": ["warn"],
    "no-useless-return": ["off"],
    "spaced-comment": ["off"],
    "react-hooks/exhaustive-deps": ["off"],
    "react/react-in-jsx-scope": "off",
    "no-console": "error",
    "react-refresh/only-export-components": ["warn", { allowConstantExport: true }],
  },
  settings: {
    react: {
      version: "detect", // React version. "detect" automatically picks the version you have installed.
    },
  },
};
