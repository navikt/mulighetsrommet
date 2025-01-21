import type { Meta, StoryObj } from "@storybook/react";

import { Metadata } from "./Metadata";

const meta: Meta<typeof Metadata> = {
  component: Metadata,
};

type Story = StoryObj<typeof Metadata>;

export const MetadataStory: Story = {
  tags: ["autodocs"],
  args: {
    header: "Navn",
    verdi: "Ola Nordmann",
  },
};

export default meta;
