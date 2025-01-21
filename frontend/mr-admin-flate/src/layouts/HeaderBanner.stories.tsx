import type { Meta, StoryObj } from "@storybook/react";

import { HeaderBanner } from "./HeaderBanner";

//👇 This default export determines where your story goes in the story list
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
    //👇 The args you need here will depend on your component
  },
};

export default meta;
