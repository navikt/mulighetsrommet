import js from "@eslint/js";
import ts from "typescript-eslint";
import pluginPromise from "eslint-plugin-promise";
import prettier from "eslint-plugin-prettier";
import react from "eslint-plugin-react";
import reactHooks from "eslint-plugin-react-hooks";
import reactRefresh from "eslint-plugin-react-refresh";
import pluginQuery from "@tanstack/eslint-plugin-query";

export default ts.config(
  js.configs.recommended,
  pluginPromise.configs["flat/recommended"],
  ...pluginQuery.configs['flat/recommended'],
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
