import type { Meta, StoryObj } from "@storybook/react";

import { AvtalestatusTag } from "./AvtalestatusTag";
import { mockAvtaler } from "@/mocks/fixtures/mock_avtaler";

const meta: Meta<typeof AvtalestatusTag> = {
  component: AvtalestatusTag,
};

type Story = StoryObj<typeof AvtalestatusTag>;

export const AvtalestatusTagStory: Story = {
  tags: ["autodocs"],
  args: {
    avtale: mockAvtaler[0],
    showAvbruttAarsak: true,
  },
};

export default meta;
