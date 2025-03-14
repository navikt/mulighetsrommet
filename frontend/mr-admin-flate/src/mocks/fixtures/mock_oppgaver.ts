import { Oppgave, OppgaveIcon, OppgaveType, Tiltakskode } from "@mr/api-client-v2";

export const mockOppgaver: Oppgave[] = [
  {
    type: OppgaveType.TILSAGN_TIL_ANNULLERING,
    title: "Tilsagn til beslutning",
    description: "Tilsagn opprettet av Benny Beslutter er klar og venter annullering",
    tiltakstype: {
      tiltakskode: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
      navn: "Arbeidsforberedende trening",
    },
    link: {
      linkText: "Gå til tilsagnet",
      link: "https://nav.no/",
    },
    createdAt: new Date(Date.now() - 5 * 24 * 60 * 60 * 1000).toString(),
    oppgaveIcon: OppgaveIcon.TILSAGN,
  },
  {
    type: OppgaveType.TILSAGN_TIL_GODKJENNING,
    title: "Send tilsagn til beslutning",
    description: "Tilsagn opprettet av Benny Beslutter er klar og venter beslutning",
    tiltakstype: {
      tiltakskode: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
      navn: "Arbeidsforberedende trening",
    },
    link: {
      linkText: "Gå til tilsagnet",
      link: "https://nav.no/",
    },
    createdAt: new Date(Date.now() - 4 * 24 * 60 * 60 * 1000).toString(),
    oppgaveIcon: OppgaveIcon.TILSAGN,
  },
  {
    type: OppgaveType.TILSAGN_TIL_GODKJENNING,
    title: "Send tilsagn til beslutning",
    description: "Tilsagn opprettet av Benny Beslutter er klar og venter beslutning",
    tiltakstype: {
      tiltakskode: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
      navn: "Arbeidsforberedende trening",
    },
    link: {
      linkText: "Gå til tilsagnet",
      link: "https://nav.no/",
    },
    createdAt: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000).toString(),
    oppgaveIcon: OppgaveIcon.TILSAGN,
  },
  {
    type: OppgaveType.UTBETALING_TIL_GODKJENNING,
    title: "Utbetaling til godkjenning",
    description: `Utbetaling for <gjennomføringsnavn> er sendt til godkjenning`,
    tiltakstype: {
      tiltakskode: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
      navn: "Arbeidsforberedende trening",
    },
    link: {
      linkText: "Se utbetaling",
      link: "https://nav.no/",
    },
    createdAt: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000).toString(),
    oppgaveIcon: OppgaveIcon.UTBETALING,
  },
];
