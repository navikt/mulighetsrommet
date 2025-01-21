import type { Meta, StoryObj } from "@storybook/react";

import { Header } from "./Header";

//👇 This default export determines where your story goes in the story list
const meta: Meta<typeof Header> = {
  component: Header,
};

type Story = StoryObj<typeof Header>;

export const HeaderStory: Story = {
  tags: ["autodocs"],
  args: {
    children: "Header innhold",
    //👇 The args you need here will depend on your component
  },
};

export default meta;
