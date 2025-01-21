import type { Meta, StoryObj } from "@storybook/react";

import { EmptyState } from "./EmptyState";

//👇 This default export determines where your story goes in the story list
const meta: Meta<typeof EmptyState> = {
  component: EmptyState,
};

type Story = StoryObj<typeof EmptyState>;

export const EmptyStateStory: Story = {
  tags: ["autodocs"],
  args: {
    tittel: "Du har ingen notifikasjoner",
    beskrivelse: "Notifikasjoner vil dukke opp her",
    //👇 The args you need here will depend on your component
  },
};

export default meta;
