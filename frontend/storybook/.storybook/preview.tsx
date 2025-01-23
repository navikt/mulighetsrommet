import type { Preview } from "@storybook/react";
import "../../mr-admin-flate/src/index.css";
import { MemoryRouter } from "react-router";

export const decorators = [
  (Story) => (
    <MemoryRouter>
      <Story />
    </MemoryRouter>
  ),
];

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
