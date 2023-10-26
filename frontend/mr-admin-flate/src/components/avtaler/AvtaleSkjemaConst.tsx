import { InferredAvtaleSchema } from "./AvtaleSchema";
import {
  Avtale,
  LeverandorUnderenhet,
  NavAnsatt,
  NavEnhet,
  NavEnhetType,
  UtkastDto,
  UtkastRequest as Utkast,
  Virksomhet,
  Avtaletype,
  Opphav,
} from "mulighetsrommet-api-client";
import { MutableRefObject } from "react";
import { UseMutationResult } from "@tanstack/react-query";
import { DeepPartial } from "react-hook-form";

export type AvtaleUtkastData = Partial<InferredAvtaleSchema> & {
  avtaleId: string;
  id: string;
};

export const saveUtkast = (
  values: InferredAvtaleSchema,
  avtale: Avtale,
  ansatt: NavAnsatt,
  utkastIdRef: MutableRefObject<string>,
  mutationUtkast: UseMutationResult<UtkastDto, unknown, Utkast, unknown>,
  setLagreState: (state: string) => void,
) => {
  const utkastData: AvtaleUtkastData = {
    ...values,
    avtaleId: avtale?.id || utkastIdRef.current,
    id: avtale?.id || utkastIdRef.current,
  };

  if (!values.navn) {
    setLagreState("For å lagre utkast må du gi utkastet et navn");
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

export const getLokaleUnderenheterAsSelectOptions = (
  navRegioner: string[],
  enheter: NavEnhet[],
) => {
  return enheter
    .filter((enhet: NavEnhet) => {
      return (
        enhet.overordnetEnhet != null &&
        navRegioner.includes(enhet.overordnetEnhet) &&
        enhet.type === NavEnhetType.LOKAL
      );
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

export function utkastDataEllerDefault(
  ansatt: NavAnsatt,
  utkast?: AvtaleUtkastData,
  avtale?: Avtale,
): DeepPartial<InferredAvtaleSchema> {
  const navRegioner = avtale?.kontorstruktur.map((struktur) => struktur.region.enhetsnummer) ?? [];
  const navEnheter =
    avtale?.kontorstruktur
      .flatMap((struktur) => struktur.kontorer)
      .map((enhet) => enhet.enhetsnummer) ?? [];
  return {
    tiltakstype: avtale?.tiltakstype,
    navRegioner,
    navEnheter,
    administrator: avtale?.administrator?.navIdent || ansatt.navIdent || "",
    navn: avtale?.navn ?? "",
    avtaletype: avtale?.avtaletype ?? Avtaletype.AVTALE,
    leverandor: avtale?.leverandor?.organisasjonsnummer ?? "",
    leverandorUnderenheter:
      avtale?.leverandorUnderenheter?.length === 0 || !avtale?.leverandorUnderenheter
        ? []
        : avtale?.leverandorUnderenheter?.map(
            (leverandor: LeverandorUnderenhet) => leverandor.organisasjonsnummer,
          ),
    leverandorKontaktpersonId: avtale?.leverandorKontaktperson?.id,
    startOgSluttDato: {
      startDato: avtale?.startDato ? avtale.startDato : undefined,
      sluttDato: avtale?.sluttDato ? avtale.sluttDato : undefined,
    },
    url: avtale?.url ?? undefined,
    prisbetingelser: avtale?.prisbetingelser ?? undefined,
    opphav: avtale?.opphav ?? Opphav.MR_ADMIN_FLATE,
    ...utkast,
  };
}
