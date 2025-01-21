import type { Meta, StoryObj } from "@storybook/react";

import { Notifikasjon } from "./Notifikasjon";

//👇 This default export determines where your story goes in the story list
const meta: Meta<typeof Notifikasjon> = {
  component: Notifikasjon,
};

type Story = StoryObj<typeof Notifikasjon>;

export const NotifikasjonStory: Story = {
  tags: ["autodocs"],
  args: {
    tittel: "Avtalen Oppfølging, tjenesteområde E - Moss utløper den 31.07.2023",
    melding:
      "Beskrivelse med en ganske lang tekst fordi vi har så komplekse prosesser så det viktig at det blir gitt nøyaktig beskjed om hva som skal til for å løse denne vanskelige problemstillingen.",
    href: "https://nav.no",
    //👇 The args you need here will depend on your component
  },
};

export default meta;
