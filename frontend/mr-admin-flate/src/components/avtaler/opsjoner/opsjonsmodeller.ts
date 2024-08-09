import { OpsjonsmodellData, OpsjonsmodellKey } from "mulighetsrommet-api-client";

export interface Opsjonsmodell {
  value: OpsjonsmodellKey;
  label: string;
  maksVarighetAar: number | null;
  initialSluttdatoEkstraAar?: number;
  kreverMaksVarighet: boolean;
}

export const opsjonsmodeller: Opsjonsmodell[] = [
  {
    value: OpsjonsmodellKey.TO_PLUSS_EN_PLUSS_EN_PLUSS_EN,
    label: "2 år + 1 år + 1 år + 1 år",
    maksVarighetAar: 5,
    initialSluttdatoEkstraAar: 2,
    kreverMaksVarighet: true,
  },
  {
    value: OpsjonsmodellKey.TO_PLUSS_EN_PLUSS_EN,
    label: "2 år + 1 år + 1 år",
    maksVarighetAar: 4,
    initialSluttdatoEkstraAar: 2,
    kreverMaksVarighet: true,
  },
  {
    value: OpsjonsmodellKey.TO_PLUSS_EN,
    label: "2 år + 1 år",
    maksVarighetAar: 3,
    initialSluttdatoEkstraAar: 2,
    kreverMaksVarighet: true,
  },
  {
    value: OpsjonsmodellKey.ANNET,
    label: "Annen opsjonsmodell",
    maksVarighetAar: 5,
    initialSluttdatoEkstraAar: undefined,
    kreverMaksVarighet: true,
  },
  {
    value: OpsjonsmodellKey.AVTALE_UTEN_OPSJONSMODELL,
    label: "Avtale uten opsjonsmodell",
    maksVarighetAar: null,
    initialSluttdatoEkstraAar: undefined,
    kreverMaksVarighet: false,
  },
  {
    value: OpsjonsmodellKey.AVTALE_VALGFRI_SLUTTDATO,
    label: "Åpen avtale med valgfri sluttdato",
    maksVarighetAar: null,
    initialSluttdatoEkstraAar: undefined,
    kreverMaksVarighet: false,
  },
];

export function opsjonsmodellTilTekst(
  opsjonsmodell: OpsjonsmodellData | undefined,
): string | null | undefined {
  const opsjonsmodellFunnet = opsjonsmodeller.find(
    (modell) => modell.value === opsjonsmodell?.opsjonsmodell,
  );

  return opsjonsmodell?.customOpsjonsmodellNavn || opsjonsmodellFunnet?.label;
}
