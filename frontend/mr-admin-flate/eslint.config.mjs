import js from "@eslint/js";
import ts from "typescript-eslint";
import prettier from "eslint-plugin-prettier";
import react from "eslint-plugin-react";
import reactHooks from "eslint-plugin-react-hooks";
import reactRefresh from "eslint-plugin-react-refresh";

export default ts.config(
  {
    ignores: ["**/dist", "**/.eslintrc.cjs"],
  },
  js.configs.recommended,
  ...ts.configs.recommended,
  {
    ...react.configs.flat.recommended,
    plugins: {
      react,
      "react-refresh": reactRefresh,
      "react-hooks": reactHooks,
    },
    settings: {
      react: {
        version: "detect",
      },
    },
    rules: {
      "react/react-in-jsx-scope": "off",
      "react-refresh/only-export-components": ["warn", { allowConstantExport: true }],
    },
  },
  {
    plugins: {
      prettier,
    },
    rules: {
      "prettier/prettier": "error",
    },
  },
  {
    rules: {
      "no-console": "error",

      "@typescript-eslint/no-use-before-define": ["off"],
      "@typescript-eslint/no-unused-vars": ["warn"],
      "@typescript-eslint/no-explicit-any": ["off"],
    },
  },
);
