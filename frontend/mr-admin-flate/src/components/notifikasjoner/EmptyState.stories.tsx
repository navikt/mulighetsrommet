import type { Meta, StoryObj } from "@storybook/react";

import { EmptyState } from "./EmptyState";

const meta: Meta<typeof EmptyState> = {
  component: EmptyState,
};

type Story = StoryObj<typeof EmptyState>;

export const EmptyStateStory: Story = {
  tags: ["autodocs"],
  args: {
    tittel: "Du har ingen notifikasjoner",
    beskrivelse: "Notifikasjoner vil dukke opp her",
  },
};

export default meta;
