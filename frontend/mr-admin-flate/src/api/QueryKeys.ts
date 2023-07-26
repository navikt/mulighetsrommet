import {
  NotificationStatus,
  SorteringTiltakstyper,
  Tiltakstypekategori,
  Tiltakstypestatus,
  Utkast,
  VirksomhetTil,
} from "mulighetsrommet-api-client";
import { AvtaleFilterProps, Tiltaksgjennomforingfilter } from "./atoms";

export const QueryKeys = {
  tiltakstype: (id?: string) => [id, "tiltakstype"] as const,
  nokkeltallTiltakstype: (id?: string) =>
    [id, "nokkeltallTiltakstype"] as const,
  tiltakstyper: (
    sokestreng: string,
    status: Tiltakstypestatus | "",
    kategori?: Tiltakstypekategori | "",
    sortering?: SorteringTiltakstyper,
    page?: number,
  ) => [sokestreng, status, kategori, sortering, page, "tiltakstyper"] as const,
  tiltaksgjennomforinger: (
    tiltaksgjennomforingfilter: Tiltaksgjennomforingfilter,
    page?: number,
  ) =>
    [
      { ...tiltaksgjennomforingfilter },
      page,
      "tiltaksgjennomforinger",
    ] as const,
  nokkeltallTiltaksgjennomforing: (id?: string) =>
    [id, "nokkeltallTiltaksgjennomforing"] as const,
  tiltaksgjennomforing: (id?: string) => [id, "tiltaksgjennomforing"] as const,
  ansatt: () => ["ansatt"] as const,
  tiltaksgjennomforingerByEnhet: (enhet: string = "enhet", page?: number) =>
    [enhet, page, "tiltaksgjennomforinger"] as const,
  avtaler: (avtaleFilter: AvtaleFilterProps, page: number) =>
    [{ ...avtaleFilter }, page, "avtaler"] as const,
  avtale: (avtaleId: string) => [avtaleId, "avtale"],
  nokkeltallAvtale: (avtaleId: string) => [avtaleId, "nokkeltallAvtale"],
  enheter: () => ["enheter"],
  virksomheter: (til?: VirksomhetTil) => [til, "virksomheter"],
  antallUlesteNotifikasjoner: () => ["antallUlesteNotifikasjoner"],
  notifikasjonerForAnsatt: (status: NotificationStatus) =>
    ["notifikasjoner", status] as const,
  virksomhetSok: (sokestreng: string) => ["virksomhetSok", sokestreng],
  virksomhetOppslag: (orgnr: string) => ["virksometOppslag", orgnr],
  tiltaksgjennomforingerTilAvtale: (filter: string) =>
    ["tiltaksgjennomforinger", filter] as const,
  kontaktpersoner: () => ["nav-kontaktpersoner"],
  betabrukere: () => ["nav-betabrukere"],
  virksomhetKontaktpersoner: (orgnr: string) =>
    [orgnr, "virksomhet-kontaktpersoner"] as const,
  alleUtkast: (avtaleId: string = "") =>
    ["utkast", "alleUtkast", avtaleId] as const,
  mineUtkast: (avtaleId?: string, utkasttype?: Utkast.type) =>
    ["utkast", avtaleId, utkasttype] as const,
  utkast: (utkastId: string) => ["utkast", utkastId] as const,
  avtalenotater: (avtaleId: string) => ["avtalenotater", avtaleId] as const,
  mineAvtalenotater: (avtaleId: string) =>
    ["avtalenotater", "mine", avtaleId] as const,
  tiltaksgjennomforingsnotater: (tiltaksgjennomforingsId: string) =>
    ["tiltaksgjennomforingsnotater", tiltaksgjennomforingsId] as const,
  mineTiltaksgjennomforingsnotater: (tiltaksgjennomforingsId: string) =>
    ["tiltaksgjennomforingsnotater", "mine", tiltaksgjennomforingsId] as const,
  features: (feature: string) => [feature, "feature"],
};
