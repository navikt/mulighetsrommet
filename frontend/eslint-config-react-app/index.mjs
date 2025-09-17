import eslint from "@eslint/js";
import tseslint from "typescript-eslint";
import pluginPromise from "eslint-plugin-promise";
import prettier from "eslint-plugin-prettier";
import react from "eslint-plugin-react";
import reactHooks from "eslint-plugin-react-hooks";
import reactRefresh from "eslint-plugin-react-refresh";
import pluginQuery from "@tanstack/eslint-plugin-query";
import { defineConfig } from "eslint/config";

export default defineConfig(
  eslint.configs.recommended,
  pluginPromise.configs["flat/recommended"],
  ...pluginQuery.configs["flat/recommended"],
  tseslint.configs.strict,
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

      "react-hooks/rules-of-hooks": "error",
      "react-hooks/exhaustive-deps": "warn",

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

      "@typescript-eslint/no-explicit-any": ["off"],
    },
  },
);
