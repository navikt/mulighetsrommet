import { Oppgave, OppgaveIconType, OppgaveType, Tiltakskode } from "@mr/api-client-v2";
import { subDays } from "date-fns";

export const mockOppgaver: Oppgave[] = [
  {
    type: OppgaveType.TILSAGN_TIL_ANNULLERING,
    navn: "Tilsagn til annullering",
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
    createdAt: subDays(Date.now(),5).toISOString(),
    iconType: OppgaveIconType.TILSAGN,
  },
  {
    type: OppgaveType.TILSAGN_TIL_GODKJENNING,
    navn: "Tilsagn til godkjenning",
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
    createdAt: subDays(Date.now(), 4).toISOString(),
    iconType: OppgaveIconType.TILSAGN,
  },
  {
    type: OppgaveType.TILSAGN_TIL_GODKJENNING,
    navn: "Tilsagn til godkjenning",
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
    createdAt: subDays(Date.now(),3).toISOString(),
    iconType: OppgaveIconType.TILSAGN,
  },
  {
    type: OppgaveType.UTBETALING_TIL_ATTESTERING,
    navn: "Utbetaling til attestering",
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
    createdAt: subDays(Date.now(), 3).toISOString(),
    iconType: OppgaveIconType.UTBETALING,
  },
];
