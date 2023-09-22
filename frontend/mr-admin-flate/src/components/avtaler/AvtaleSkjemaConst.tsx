import { InferredAvtaleSchema } from "./AvtaleSchema";
import { toast } from "react-toastify";
import {
  Avtale,
  LeverandorUnderenhet,
  NavAnsatt,
  NavEnhet,
  NavEnhetType,
  Utkast,
  Virksomhet,
} from "mulighetsrommet-api-client";
import { MutableRefObject } from "react";
import { UseMutationResult } from "@tanstack/react-query";

type UtkastData = Pick<
  Avtale,
  | "navn"
  | "tiltakstype"
  | "navRegion"
  | "navEnheter"
  | "administrator"
  | "avtaletype"
  | "leverandor"
  | "leverandorUnderenheter"
  | "leverandorKontaktperson"
  | "startDato"
  | "sluttDato"
  | "url"
  | "prisbetingelser"
> & {
  avtaleId: string;
  id: string;
};

export const saveUtkast = (
  values: InferredAvtaleSchema,
  avtale: Avtale,
  ansatt: NavAnsatt,
  utkastIdRef: MutableRefObject<string>,
  mutationUtkast: UseMutationResult<Utkast, unknown, Utkast, unknown>,
) => {
  const utkastData: UtkastData = {
    navn: values?.avtalenavn,
    tiltakstype: {
      id: values?.tiltakstype,
      arenaKode: "",
      navn: "",
    },
    navRegion: {
      navn: "",
      enhetsnummer: values?.navRegion,
    },
    navEnheter: values?.navEnheter?.map((enhetsnummer) => ({
      navn: "",
      enhetsnummer,
    })),
    administrator: { navIdent: values?.administrator, navn: "" },
    avtaletype: values?.avtaletype,
    leverandor: {
      navn: "",
      organisasjonsnummer: values?.leverandor,
      slettet: false,
    },
    leverandorUnderenheter: values?.leverandorUnderenheter?.map((organisasjonsnummer) => ({
      navn: "",
      organisasjonsnummer,
    })),
    startDato: values?.startOgSluttDato?.startDato?.toDateString(),
    sluttDato: values?.startOgSluttDato?.sluttDato?.toDateString(),
    url: values?.url,
    prisbetingelser: values?.prisOgBetalingsinfo || "",
    avtaleId: avtale?.id || utkastIdRef.current,
    id: avtale?.id || utkastIdRef.current,
  };

  if (!values.avtalenavn) {
    toast.info("For å lagre utkast må du gi utkastet et navn", {
      autoClose: 10000,
    });
    return;
  }

  mutationUtkast.mutate({
    id: utkastIdRef.current,
    utkastData,
    type: Utkast.type.AVTALE,
    opprettetAv: ansatt?.navIdent,
    avtaleId: utkastIdRef.current,
  });
};

export const defaultEnhet = (
  avtale: Avtale | undefined,
  enheter: NavEnhet[],
  ansatt: NavAnsatt,
) => {
  if (avtale?.navRegion?.enhetsnummer) {
    return avtale?.navRegion?.enhetsnummer;
  }
  if (enheter.find((e) => e.enhetsnummer === ansatt.hovedenhet.enhetsnummer)) {
    return ansatt.hovedenhet.enhetsnummer;
  }
  return undefined;
};

export const getLokaleUnderenheterAsSelectOptions = (
  navRegion: string | undefined,
  enheter: NavEnhet[],
) => {
  if (!navRegion) {
    return [];
  }

  return enheter
    .filter((enhet: NavEnhet) => {
      return navRegion === enhet.overordnetEnhet && enhet.type === NavEnhetType.LOKAL;
    })
    .map((enhet: NavEnhet) => ({
      label: enhet.navn,
      value: enhet.enhetsnummer,
    }));
};

export const underenheterOptions = (underenheterForLeverandor: Virksomhet[]) =>
  underenheterForLeverandor.map((leverandor: LeverandorUnderenhet) => ({
    value: leverandor.organisasjonsnummer,
    label: `${leverandor.navn} - ${leverandor.organisasjonsnummer}`,
  }));
