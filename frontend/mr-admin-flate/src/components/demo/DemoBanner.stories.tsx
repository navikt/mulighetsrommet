import type { Meta, StoryObj } from "@storybook/react";

import { DemoBanner } from "./DemoBanner";

const meta: Meta<typeof DemoBanner> = {
  component: DemoBanner,
};

type Story = StoryObj<typeof DemoBanner>;

export const AvtalestatusTagStory: Story = {
  tags: ["autodocs"],
  args: {},
};

export default meta;
