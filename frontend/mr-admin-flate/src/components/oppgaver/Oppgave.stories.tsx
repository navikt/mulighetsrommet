import type { Meta, StoryObj } from "@storybook/react";

import { Oppgave } from "./Oppgave";
import { mockOppgaver } from "@/mocks/fixtures/mock_oppgaver";
import { mockTiltakstyper } from "@/mocks/fixtures/mock_tiltakstyper";

//👇 This default export determines where your story goes in the story list
const meta: Meta<typeof Oppgave> = {
  component: Oppgave,
};

type Story = StoryObj<typeof Oppgave>;

export const OppgaveStory: Story = {
  args: {
    oppgave: mockOppgaver[0],
    tiltakstype: mockTiltakstyper.ARBFORB,
    //👇 The args you need here will depend on your component
  },
};

export default meta;
