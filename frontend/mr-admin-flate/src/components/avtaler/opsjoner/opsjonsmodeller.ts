import { Avtaletype, OpsjonsmodellType } from "@mr/api-client-v2";

export interface TilgjengeligOpsjonsmodell {
  value: OpsjonsmodellType;
  label: string;
  maksVarighetAar: number | null;
  initialSluttdatoEkstraAar?: number;
  kreverMaksVarighet: boolean;
}

export function hentGjeldendeOpsjonsmodeller(avtaletype: Avtaletype): TilgjengeligOpsjonsmodell[] {
  if (avtaletype === Avtaletype.FORHANDSGODKJENT) {
    return opsjonsmodeller.filter(
      (modell) => modell.value === OpsjonsmodellType.AVTALE_VALGFRI_SLUTTDATO,
    );
  }

  if (avtaletype !== Avtaletype.OFFENTLIG_OFFENTLIG) {
    return opsjonsmodeller.filter(
      (modell) => modell.value !== OpsjonsmodellType.AVTALE_VALGFRI_SLUTTDATO,
    );
  }

  return opsjonsmodeller;
}

export function hentOpsjonsmodell(type: OpsjonsmodellType) {
  return opsjonsmodeller.find((modell) => modell.value === type);
}

const opsjonsmodeller: TilgjengeligOpsjonsmodell[] = [
  {
    value: OpsjonsmodellType.TO_PLUSS_EN_PLUSS_EN_PLUSS_EN,
    label: "2 år + 1 år + 1 år + 1 år",
    maksVarighetAar: 5,
    initialSluttdatoEkstraAar: 2,
    kreverMaksVarighet: true,
  },
  {
    value: OpsjonsmodellType.TO_PLUSS_EN_PLUSS_EN,
    label: "2 år + 1 år + 1 år",
    maksVarighetAar: 4,
    initialSluttdatoEkstraAar: 2,
    kreverMaksVarighet: true,
  },
  {
    value: OpsjonsmodellType.TO_PLUSS_EN,
    label: "2 år + 1 år",
    maksVarighetAar: 3,
    initialSluttdatoEkstraAar: 2,
    kreverMaksVarighet: true,
  },
  {
    value: OpsjonsmodellType.ANNET,
    label: "Annen opsjonsmodell",
    maksVarighetAar: null,
    initialSluttdatoEkstraAar: undefined,
    kreverMaksVarighet: true,
  },
  {
    value: OpsjonsmodellType.AVTALE_UTEN_OPSJONSMODELL,
    label: "Avtale uten opsjonsmulighet",
    maksVarighetAar: null,
    initialSluttdatoEkstraAar: undefined,
    kreverMaksVarighet: false,
  },
  {
    value: OpsjonsmodellType.AVTALE_VALGFRI_SLUTTDATO,
    label: "Åpen avtale med valgfri sluttdato",
    maksVarighetAar: null,
    initialSluttdatoEkstraAar: undefined,
    kreverMaksVarighet: false,
  },
];
