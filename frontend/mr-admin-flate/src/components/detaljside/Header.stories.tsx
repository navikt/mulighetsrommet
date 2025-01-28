import type { Meta, StoryObj } from "@storybook/react";

import { Header } from "./Header";

const meta: Meta<typeof Header> = {
  component: Header,
};

type Story = StoryObj<typeof Header>;

export const HeaderStory: Story = {
  tags: ["autodocs"],
  args: {
    children: "Header innhold",
  },
};

export default meta;
