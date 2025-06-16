import type { Meta, StoryObj } from "@storybook/react";

import { TiltakstypeStatusTag } from "./TiltakstypeStatusTag";
import { TiltakstypeStatus } from "@mr/api-client-v2";

const meta: Meta<typeof TiltakstypeStatusTag> = {
  component: TiltakstypeStatusTag,
};

type Story = StoryObj<typeof TiltakstypeStatusTag>;

export const TiltakstypeStatusTagStory: Story = {
  tags: ["autodocs"],
  args: {
    status: TiltakstypeStatus.AKTIV,
  },
};

export default meta;
