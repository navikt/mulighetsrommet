import type { Meta, StoryObj } from "@storybook/react";

import { Laster } from "./Laster";

//👇 This default export determines where your story goes in the story list
const meta: Meta<typeof Laster> = {
  component: Laster,
};

type Story = StoryObj<typeof Laster>;

export const LasterStory: Story = {
  tags: ["autodocs"],
  args: {
    tekst: "Laster innhold",
    size: "medium",
    //👇 The args you need here will depend on your component
  },
};

export default meta;
