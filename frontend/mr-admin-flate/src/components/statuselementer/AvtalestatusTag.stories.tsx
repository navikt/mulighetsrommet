import type { Meta, StoryObj } from "@storybook/react";

import { AvtalestatusTag } from "./AvtalestatusTag";
import { mockAvtaler } from "@/mocks/fixtures/mock_avtaler";

//👇 This default export determines where your story goes in the story list
const meta: Meta<typeof AvtalestatusTag> = {
  component: AvtalestatusTag,
};

type Story = StoryObj<typeof AvtalestatusTag>;

export const AvtalestatusTagStory: Story = {
  tags: ["autodocs"],
  args: {
    avtale: mockAvtaler[0],
    showAvbruttAarsak: true,
    //👇 The args you need here will depend on your component
  },
};

export default meta;
