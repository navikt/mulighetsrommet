import type { Meta, StoryObj } from "@storybook/react";

import { TiltakstypestatusTag } from "./TiltakstypestatusTag";
import { mockTiltakstyper } from "@/mocks/fixtures/mock_tiltakstyper";

//👇 This default export determines where your story goes in the story list
const meta: Meta<typeof TiltakstypestatusTag> = {
  component: TiltakstypestatusTag,
};

type Story = StoryObj<typeof TiltakstypestatusTag>;

export const TiltakstypestatusTagStory: Story = {
  tags: ["autodocs"],
  args: {
    tiltakstype: mockTiltakstyper.ARBFORB,
    //👇 The args you need here will depend on your component
  },
};

export default meta;
