import type { Meta, StoryObj } from "@storybook/react";

import { Oppgave } from "./Oppgave";
import { mockOppgaver } from "@/mocks/fixtures/mock_oppgaver";
import { mockTiltakstyper } from "@/mocks/fixtures/mock_tiltakstyper";

const meta: Meta<typeof Oppgave> = {
  component: Oppgave,
};

type Story = StoryObj<typeof Oppgave>;

export const OppgaveStory: Story = {
  args: {
    oppgave: mockOppgaver[0],
    tiltakstype: mockTiltakstyper.ARBFORB,
  },
};

export default meta;
