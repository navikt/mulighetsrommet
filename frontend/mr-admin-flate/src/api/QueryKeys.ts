import { NavAnsattRolle, NotificationStatus, VirksomhetTil } from "mulighetsrommet-api-client";
import { AvtaleFilter, TiltaksgjennomforingFilter, TiltakstypeFilter } from "./atoms";

export const QueryKeys = {
  tiltakstype: (id?: string) => ["tiltakstype", id] as const,
  tiltakstyper: (sokestreng?: string, filter?: TiltakstypeFilter, page?: number) =>
    ["tiltakstyper", page, sokestreng, { ...filter }] as const,
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
  tiltaksgjennomforingerByEnhet: (enhet: string = "enhet", page?: number) =>
    [enhet, page, "tiltaksgjennomforinger"] as const,
  ansatt: () => ["ansatt"] as const,
  avtaler: (mine?: boolean, page?: number, avtaleFilter?: Partial<AvtaleFilter>) =>
    ["avtaler", mine, page, { ...avtaleFilter }] as const,
  avtale: (id: string) => ["avtale", id],
  avtaleHistorikk: (id?: string) => ["avtale", id, "historikk"] as const,
  enheter: () => ["enheter"],
  virksomheter: (til?: VirksomhetTil) => ["virksomheter", til],
  antallUlesteNotifikasjoner: () => ["antallUlesteNotifikasjoner"],
  notifikasjonerForAnsatt: (status: NotificationStatus) => ["notifikasjoner", status] as const,
  virksomhetSok: (sokestreng: string) => ["virksomhet", "sok", sokestreng],
  virksomhet: (id: string) => ["virksomet", id],
  navansatt: (rolle: NavAnsattRolle) => ["nav-ansatte", rolle],
  virksomhetKontaktpersoner: (virksomhetId: string) =>
    ["virksomhet", virksomhetId, "kontaktpersoner"] as const,
  avtalenotater: (avtaleId: string) => ["avtalenotater", avtaleId] as const,
  mineAvtalenotater: (avtaleId: string) => ["avtalenotater", "mine", avtaleId] as const,
  tiltaksgjennomforingsnotater: (tiltaksgjennomforingsId: string) =>
    ["tiltaksgjennomforingsnotater", tiltaksgjennomforingsId] as const,
  mineTiltaksgjennomforingsnotater: (tiltaksgjennomforingsId: string) =>
    ["tiltaksgjennomforingsnotater", "mine", tiltaksgjennomforingsId] as const,
  features: (feature: string) => ["feature", feature],
  migrerteTiltakstyper: () => ["migrerteTiltakstyper"],
};
