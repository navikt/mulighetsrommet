import type { Meta, StoryObj } from "@storybook/react";

import { TiltakstypeStatusTag } from "./TiltakstypeStatusTag";
import { mockTiltakstyper } from "@/mocks/fixtures/mock_tiltakstyper";

const meta: Meta<typeof TiltakstypeStatusTag> = {
  component: TiltakstypeStatusTag,
};

type Story = StoryObj<typeof TiltakstypeStatusTag>;

export const TiltakstypeStatusTagStory: Story = {
  tags: ["autodocs"],
  args: {
    tiltakstype: mockTiltakstyper.ARBFORB,
  },
};

export default meta;
