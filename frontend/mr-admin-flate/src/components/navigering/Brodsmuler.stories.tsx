import type { Meta, StoryObj } from "@storybook/react";

import { Brodsmuler } from "./Brodsmuler";

const meta: Meta<typeof Brodsmuler> = {
  component: Brodsmuler,
};

type Story = StoryObj<typeof Brodsmuler>;

export const BrodsmulerStory: Story = {
  tags: ["autodocs"],
  args: {
    brodsmuler: [
      {
        tittel: "Avtaler",
        lenke: "/avtaler",
      },
      {
        tittel: "Gjennomføringer",
        lenke: "/gjennomforinger",
      },
    ],
  },
};

export default meta;
