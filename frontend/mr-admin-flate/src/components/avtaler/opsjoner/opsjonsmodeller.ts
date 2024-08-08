import { OpsjonsmodellData, OpsjonsmodellKey } from "mulighetsrommet-api-client";

export interface Opsjonsmodell {
  value: OpsjonsmodellKey;
  label: string;
  maksVarighetAar: number | null;
  initialSluttdatoEkstraAar?: number;
}

export const opsjonsmodeller: Opsjonsmodell[] = [
  {
    value: OpsjonsmodellKey.TO_PLUSS_EN_PLUSS_EN_PLUSS_EN,
    label: "2 år + 1 år + 1 år + 1 år",
    maksVarighetAar: 5,
    initialSluttdatoEkstraAar: 2,
  },
  {
    value: OpsjonsmodellKey.TO_PLUSS_EN_PLUSS_EN,
    label: "2 år + 1 år + 1 år",
    maksVarighetAar: 4,
    initialSluttdatoEkstraAar: 2,
  },
  {
    value: OpsjonsmodellKey.TO_PLUSS_EN,
    label: "2 år + 1 år",
    maksVarighetAar: 3,
    initialSluttdatoEkstraAar: 2,
  },
  {
    value: OpsjonsmodellKey.ANNET,
    label: "Annen opsjonsmodell",
    maksVarighetAar: 5,
    initialSluttdatoEkstraAar: undefined,
  },
  {
    value: OpsjonsmodellKey.AVTALE_UTEN_OPSJONSMODELL,
    label: "Avtale uten opsjonsmodell",
    maksVarighetAar: null,
    initialSluttdatoEkstraAar: undefined,
  },
  {
    value: OpsjonsmodellKey.AVTALE_VALGFRI_SLUTTDATO,
    label: "Åpen avtale med valgfri sluttdato",
    maksVarighetAar: null,
    initialSluttdatoEkstraAar: undefined,
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
