import type { Meta, StoryObj } from "@storybook/react";

import { AvtaleStatusTag } from "./AvtaleStatusTag";

const meta: Meta<typeof AvtaleStatusTag> = {
  component: AvtaleStatusTag,
};

type Story = StoryObj<typeof AvtaleStatusTag>;

export const AvtaleStatusTagStory: Story = {
  tags: ["autodocs"],
  args: {
    status: "AKTIV",
  },
};

export default meta;
