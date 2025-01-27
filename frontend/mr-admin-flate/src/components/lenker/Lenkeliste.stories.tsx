import type { Meta, StoryObj } from "@storybook/react";

import { Lenkeliste } from "./Lenkeliste";

const meta: Meta<typeof Lenkeliste> = {
  component: Lenkeliste,
};

type Story = StoryObj<typeof Lenkeliste>;

export const LenkelisteStory: Story = {
  tags: ["autodocs"],
  args: {
    lenker: [
      {
        lenkenavn: "Arrangører",
        lenke: "/arrangorer",
        apneINyFane: false,
        visKunForVeileder: false,
      },
      {
        lenkenavn: "Gjennomføringer",
        lenke: "/gjennomforinger",
        apneINyFane: false,
        visKunForVeileder: false,
      },
      {
        lenkenavn: "Avtaler",
        lenke: "/avtaler",
        apneINyFane: false,
        visKunForVeileder: false,
      },
    ],
  },
};

export default meta;
