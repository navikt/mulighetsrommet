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
    tiltaksgjennomforingfilter?: TiltaksgjennomforingfilterProps,
    page?: number,
  ) => ["tiltaksgjennomforinger", page, { ...tiltaksgjennomforingfilter }] as const,
  tiltaksgjennomforing: (id?: string) => ["tiltaksgjennomforing", id] as const,
  tiltaksgjennomforingerByEnhet: (enhet: string = "enhet", page?: number) =>
    [enhet, page, "tiltaksgjennomforinger"] as const,
  veilederflateTiltaksgjennomforing: (id: string) => [id, "tiltaksgjennomforing"] as const,
  ansatt: () => ["ansatt"] as const,
  avtaler: (avtaleFilter: AvtaleFilterProps, page: number) =>
    ["avtaler", page, { ...avtaleFilter }] as const,
  avtale: (avtaleId: string) => ["avtale", avtaleId],
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
