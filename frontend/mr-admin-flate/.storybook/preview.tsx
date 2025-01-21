import type { Preview } from "@storybook/react";
import { Decorator } from "@storybook/react";
import "../src/index.css";
import { MemoryRouter } from "react-router";

const preview: Preview = {
  parameters: {
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/i,
      },
    },
  },
};

export default preview;
