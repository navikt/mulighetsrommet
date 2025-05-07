import type { Meta, StoryObj } from "@storybook/react";

import { LenkerList } from "./LenkerList";

const meta: Meta<typeof LenkerList> = {
  component: LenkerList,
};

type Story = StoryObj<typeof LenkerList>;

export const LenkerListStory: Story = {
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
