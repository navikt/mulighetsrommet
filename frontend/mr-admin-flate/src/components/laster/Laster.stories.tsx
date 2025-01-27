import type { Meta, StoryObj } from "@storybook/react";

import { Laster } from "./Laster";

const meta: Meta<typeof Laster> = {
  component: Laster,
};

type Story = StoryObj<typeof Laster>;

export const LasterStory: Story = {
  tags: ["autodocs"],
  args: {
    tekst: "Laster innhold",
    size: "medium",
  },
};

export default meta;
