import type { Meta, StoryObj } from "@storybook/react";

import { HeaderBanner } from "./HeaderBanner";

const meta: Meta<typeof HeaderBanner> = {
  component: HeaderBanner,
};

type Story = StoryObj<typeof HeaderBanner>;

export const HeaderBannerStory: Story = {
  tags: ["autodocs"],
  args: {
    heading: "Eksempel side",
    harUndermeny: true,
    ikon: null,
  },
};

export default meta;
