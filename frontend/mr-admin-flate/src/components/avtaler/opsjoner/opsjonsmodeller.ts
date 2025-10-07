import { OpsjonsmodellType, Avtaletype } from "@tiltaksadministrasjon/api-client";

export interface TilgjengeligOpsjonsmodell {
  type: OpsjonsmodellType;
  label: string;
  maksVarighetAar: number | null;
  initialSluttdatoEkstraAar?: number;
  kreverMaksVarighet: boolean;
}

export function hentGjeldendeOpsjonsmodeller(avtaletype: Avtaletype): TilgjengeligOpsjonsmodell[] {
  if (avtaletype === Avtaletype.FORHANDSGODKJENT) {
    return opsjonsmodeller.filter((modell) => modell.type === OpsjonsmodellType.VALGFRI_SLUTTDATO);
  }

  if (avtaletype !== Avtaletype.OFFENTLIG_OFFENTLIG) {
    return opsjonsmodeller.filter((modell) => modell.type !== OpsjonsmodellType.VALGFRI_SLUTTDATO);
  }

  return opsjonsmodeller;
}

export function hentOpsjonsmodell(type: OpsjonsmodellType) {
  return opsjonsmodeller.find((modell) => modell.type === type);
}

const opsjonsmodeller: TilgjengeligOpsjonsmodell[] = [
  {
    type: OpsjonsmodellType.TO_PLUSS_EN_PLUSS_EN_PLUSS_EN,
    label: "2 år + 1 år + 1 år + 1 år",
    maksVarighetAar: 5,
    initialSluttdatoEkstraAar: 2,
    kreverMaksVarighet: true,
  },
  {
    type: OpsjonsmodellType.TO_PLUSS_EN_PLUSS_EN,
    label: "2 år + 1 år + 1 år",
    maksVarighetAar: 4,
    initialSluttdatoEkstraAar: 2,
    kreverMaksVarighet: true,
  },
  {
    type: OpsjonsmodellType.TO_PLUSS_EN,
    label: "2 år + 1 år",
    maksVarighetAar: 3,
    initialSluttdatoEkstraAar: 2,
    kreverMaksVarighet: true,
  },
  {
    type: OpsjonsmodellType.ANNET,
    label: "Annen opsjonsmodell",
    maksVarighetAar: null,
    initialSluttdatoEkstraAar: undefined,
    kreverMaksVarighet: true,
  },
  {
    type: OpsjonsmodellType.INGEN_OPSJONSMULIGHET,
    label: "Avtale uten opsjonsmulighet",
    maksVarighetAar: null,
    initialSluttdatoEkstraAar: undefined,
    kreverMaksVarighet: false,
  },
  {
    type: OpsjonsmodellType.VALGFRI_SLUTTDATO,
    label: "Åpen avtale med valgfri sluttdato",
    maksVarighetAar: null,
    initialSluttdatoEkstraAar: undefined,
    kreverMaksVarighet: false,
  },
];
