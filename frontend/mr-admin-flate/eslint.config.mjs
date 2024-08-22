import config from "@mr/eslint-config-react-app";

export default [
  {
    ignores: ["dist", "playwright-report"],
  },
  ...config,
];
