import type { Meta, StoryObj } from "@storybook/react";

import { AvtaleStatusTag } from "./AvtaleStatusTag";
import { mockAvtaler } from "@/mocks/fixtures/mock_avtaler";

const meta: Meta<typeof AvtaleStatusTag> = {
  component: AvtaleStatusTag,
};

type Story = StoryObj<typeof AvtaleStatusTag>;

export const AvtaleStatusTagStory: Story = {
  tags: ["autodocs"],
  args: {
    avtale: mockAvtaler[0],
    showAvbruttAarsak: true,
  },
};

export default meta;
