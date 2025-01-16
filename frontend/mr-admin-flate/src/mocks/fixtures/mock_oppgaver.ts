import { Oppgave, OppgaveType, Tiltakskode } from "@mr/api-client";

export const mockOppgaver: Oppgave[] = [
  {
    type: OppgaveType.TILSAGN_TIL_ANNULLERING,
    title: "Tilsagn til beslutning",
    description: "Tilsagn opprettet av Benny Beslutter er klar og venter annullering",
    tiltakstype: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
    link: {
      linkText: "Gå til tilsagnet",
      link: "https://nav.no/",
    },
    createdAt: new Date(Date.now() - 5 * 24 * 60 * 60 * 1000).toString(),
    deadline: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toString(),
  },
  {
    type: OppgaveType.TILSAGN_TIL_BESLUTNING,
    title: "Send tilsagn til beslutning",
    description: "Tilsagn opprettet av Benny Beslutter er klar og venter beslutning",
    tiltakstype: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
    link: {
      linkText: "Gå til tilsagnet",
      link: "https://nav.no/",
    },
    createdAt: new Date(Date.now() - 4 * 24 * 60 * 60 * 1000).toString(),
    deadline: new Date(Date.now() + 6 * 24 * 60 * 60 * 1000).toString(),
  },
  {
    type: OppgaveType.TILSAGN_TIL_BESLUTNING,
    title: "Send tilsagn til beslutning",
    description: "Tilsagn opprettet av Benny Beslutter er klar og venter beslutning",
    tiltakstype: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
    link: {
      linkText: "Gå til tilsagnet",
      link: "https://nav.no/",
    },
    createdAt: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000).toString(),
    deadline: new Date(Date.now() + 5 * 24 * 60 * 60 * 1000).toString(), // 5 days from now
  },
];
