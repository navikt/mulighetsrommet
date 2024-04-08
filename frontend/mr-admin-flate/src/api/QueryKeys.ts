import { ArrangorTil, NavAnsattRolle, NotificationStatus } from "mulighetsrommet-api-client";
import { ArrangorerFilter, AvtaleFilter, TiltaksgjennomforingFilter } from "./atoms";

export const QueryKeys = {
  tiltakstype: (id?: string) => ["tiltakstype", id] as const,
  tiltakstyper: (filter?: object) => ["tiltakstyper", { ...filter }] as const,
  tiltakstypeFaneinnhold: (id: string) => ["tiltakstype", id, "faneinnhold"] as const,
  tiltaksgjennomforinger: (
    mine?: boolean,
    page?: number,
    filter?: Partial<TiltaksgjennomforingFilter>,
  ) => ["tiltaksgjennomforinger", mine, page, filter].filter((entry) => entry !== undefined),
  tiltaksgjennomforing: (id?: string) => ["tiltaksgjennomforing", id] as const,
  tiltaksgjennomforingHistorikk: (id?: string) =>
    ["tiltaksgjennomforing", id, "historikk"] as const,
  tiltaksgjennomforingDeltakerSummary(id: string) {
    return ["tiltaksgjennomforing", id, "deltaker-summary"] as const;
  },
  ansatt: () => ["ansatt"] as const,
  avtaler: (mine?: boolean, page?: number, avtaleFilter?: Partial<AvtaleFilter>) =>
    ["avtaler", mine, page, { ...avtaleFilter }] as const,
  avtale: (id: string) => ["avtale", id],
  avtaleHistorikk: (id?: string) => ["avtale", id, "historikk"] as const,
  enheter: () => ["enheter"],
  arrangorer: (til?: ArrangorTil, page?: number, arrangorFilter?: Partial<ArrangorerFilter>) => [
    "arrangorer",
    page,
    { ...arrangorFilter },
    { til },
  ],
  arrangorById: (id: string) => ["arrangor", id],
  arrangorByOrgnr: (orgnr: string) => ["arrangor", { orgnr }],
  arrangorKontaktpersoner: (id: string) => ["arrangor", id, "kontaktpersoner"],
  antallUlesteNotifikasjoner: () => ["antallUlesteNotifikasjoner"],
  notifikasjonerForAnsatt: (status: NotificationStatus) => ["notifikasjoner", status] as const,
  brregVirksomheter: (sokestreng: string) => ["virksomhet", "sok", sokestreng],
  brregVirksomhetUnderenheter: (id: string) => ["virksomet", id, "underenheter"],
  navansatt: (rolle: NavAnsattRolle) => ["nav-ansatte", rolle],
  avtalenotater: (avtaleId: string) => ["avtalenotater", avtaleId] as const,
  mineAvtalenotater: (avtaleId: string) => ["avtalenotater", "mine", avtaleId] as const,
  tiltaksgjennomforingsnotater: (tiltaksgjennomforingsId: string) =>
    ["tiltaksgjennomforingsnotater", tiltaksgjennomforingsId] as const,
  mineTiltaksgjennomforingsnotater: (tiltaksgjennomforingsId: string) =>
    ["tiltaksgjennomforingsnotater", "mine", tiltaksgjennomforingsId] as const,
  features: (feature: string) => ["feature", feature],
  migrerteTiltakstyper: () => ["migrerteTiltakstyper"],
  navRegioner: () => ["navRegioner"],
};
