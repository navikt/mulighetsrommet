import type { Meta, StoryObj } from "@storybook/react";

import { TiltakstypestatusTag } from "./TiltakstypestatusTag";
import { mockTiltakstyper } from "@/mocks/fixtures/mock_tiltakstyper";

const meta: Meta<typeof TiltakstypestatusTag> = {
  component: TiltakstypestatusTag,
};

type Story = StoryObj<typeof TiltakstypestatusTag>;

export const TiltakstypestatusTagStory: Story = {
  tags: ["autodocs"],
  args: {
    tiltakstype: mockTiltakstyper.ARBFORB,
  },
};

export default meta;
