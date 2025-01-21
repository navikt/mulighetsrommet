import type { Meta, StoryObj } from "@storybook/react";

import { Metadata } from "./Metadata";

//👇 This default export determines where your story goes in the story list
const meta: Meta<typeof Metadata> = {
  component: Metadata,
};

type Story = StoryObj<typeof Metadata>;

export const MetadataStory: Story = {
  tags: ["autodocs"],
  args: {
    header: "Navn",
    verdi: "Ola Nordmann",
    //👇 The args you need here will depend on your component
  },
};

export default meta;
