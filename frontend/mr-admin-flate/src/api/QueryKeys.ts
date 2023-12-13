import {
  NavAnsattRolle,
  NotificationStatus,
  UtkastRequest as Utkast,
  VirksomhetTil,
} from "mulighetsrommet-api-client";
import { AvtaleFilterProps, TiltaksgjennomforingfilterProps, TiltakstypeFilter } from "./atoms";

export const QueryKeys = {
  tiltakstype: (id?: string) => ["tiltakstype", id] as const,
  tiltakstyper: (sokestreng: string, filter: TiltakstypeFilter, page?: number) =>
    ["tiltakstyper", page, sokestreng, { ...filter }] as const,
  tiltaksgjennomforinger: (
    page?: number,
    tiltaksgjennomforingfilter?: TiltaksgjennomforingfilterProps,
  ) => ["tiltaksgjennomforinger", page, { ...tiltaksgjennomforingfilter }] as const,
  tiltaksgjennomforing: (id?: string) => ["tiltaksgjennomforing", id] as const,
  tiltaksgjennomforingHistorikk: (id?: string) =>
    ["tiltaksgjennomforing", id, "historikk"] as const,
  tiltaksgjennomforingerByEnhet: (enhet: string = "enhet", page?: number) =>
    [enhet, page, "tiltaksgjennomforinger"] as const,
  veilederflateTiltaksgjennomforing: (id: string) => [id, "tiltaksgjennomforing"] as const,
  ansatt: () => ["ansatt"] as const,
  avtaler: (mine?: boolean, page?: number, avtaleFilter?: AvtaleFilterProps) =>
    ["avtaler", mine, page, { ...avtaleFilter }] as const,
  avtale: (id: string) => ["avtale", id],
  avtaleHistorikk: (id?: string) => ["avtale", id, "historikk"] as const,
  enheter: () => ["enheter"],
  virksomheter: (til?: VirksomhetTil) => ["virksomheter", til],
  antallUlesteNotifikasjoner: () => ["antallUlesteNotifikasjoner"],
  notifikasjonerForAnsatt: (status: NotificationStatus) => ["notifikasjoner", status] as const,
  virksomhetSok: (sokestreng: string) => ["virksomhetSok", sokestreng],
  virksomhetOppslag: (orgnr: string) => ["virksometOppslag", orgnr],
  tiltaksgjennomforingerTilAvtale: (filter: string) => ["tiltaksgjennomforinger", filter] as const,
  kontaktpersoner: (rolle: NavAnsattRolle) => ["nav-kontaktpersoner", rolle],
  virksomhetKontaktpersoner: (orgnr: string) => ["virksomhet-kontaktpersoner", orgnr] as const,
  alleUtkast: (avtaleId: string = "") => ["utkast", "alleUtkast", avtaleId] as const,
  mineUtkast: (avtaleId?: string, utkasttype?: Utkast.type) =>
    ["utkast", avtaleId, utkasttype] as const,
  utkast: (utkastId?: string) => ["utkast", utkastId] as const,
  avtalenotater: (avtaleId: string) => ["avtalenotater", avtaleId] as const,
  mineAvtalenotater: (avtaleId: string) => ["avtalenotater", "mine", avtaleId] as const,
  tiltaksgjennomforingsnotater: (tiltaksgjennomforingsId: string) =>
    ["tiltaksgjennomforingsnotater", tiltaksgjennomforingsId] as const,
  mineTiltaksgjennomforingsnotater: (tiltaksgjennomforingsId: string) =>
    ["tiltaksgjennomforingsnotater", "mine", tiltaksgjennomforingsId] as const,
  features: (feature: string) => ["feature", feature],
};
