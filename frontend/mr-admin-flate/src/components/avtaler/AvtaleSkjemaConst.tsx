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
} from "mulighetsrommet-api-client";
import { MutableRefObject } from "react";
import { UseMutationResult } from "@tanstack/react-query";

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
