import { RsEnhet, RsEnhetInkludertKontaktinformasjon } from "norg2-api-client";

export interface SanityEnhet {
  _id: string;
  _type: "enhet";
  navn: string;
  nummer: {
    _type: "slug";
    current: string;
  };
  type: string;
  status: string;
  fylke?: {
    _type: "reference";
    _ref: string;
    _key: string;
  };
}

type AvailableEnhet = Required<
  Pick<RsEnhetInkludertKontaktinformasjon, "enhet">
>;

const relevantEnhetStatus: Array<string | undefined> = [
  "Under avvikling",
  "Aktiv",
];

type Enhetstype = "LOKAL" | "TILTAK" | "FYLKE" | "ALS";

export function spesialEnheterToSanity(
  enheter: RsEnhetInkludertKontaktinformasjon[],
  whitelistTyper: Enhetstype[]
): SanityEnhet[] {
  return enheter
    .filter(
      (enhet): enhet is AvailableEnhet =>
        whitelistTyper.includes(enhet.enhet?.type as Enhetstype) &&
        relevantEnhetStatus.includes(enhet.enhet?.status)
    )
    .map((enhet) => toSanityEnhet(enhet?.enhet));
}

export function fylkeOgUnderenheterToSanity(
  enheter: RsEnhetInkludertKontaktinformasjon[]
): SanityEnhet[] {
  return enheter
    .filter((enhet): enhet is AvailableEnhet => {
      return (
        enhet.enhet?.type === "FYLKE" &&
        relevantEnhetStatus.includes(enhet.enhet.status)
      );
    })
    .flatMap(({ enhet: fylke }) => {
      const underliggendeEnheter = enheter
        .filter((enhet): enhet is AvailableEnhet =>
          isUnderliggendeEnhet(fylke, enhet)
        )
        .map(({ enhet }) => {
          return toSanityEnhet(enhet, fylke);
        });
      return [toSanityEnhet(fylke), ...underliggendeEnheter];
    });
}

function isUnderliggendeEnhet(
  enhet: RsEnhet,
  otherEnhet: RsEnhetInkludertKontaktinformasjon
) {
  return (
    relevantEnhetStatus.includes(otherEnhet.enhet?.status) &&
    otherEnhet.overordnetEnhet === enhet?.enhetNr
  );
}

function toSanityEnhet(enhet: RsEnhet, fylke?: RsEnhet): SanityEnhet {
  const sanityEnhet: SanityEnhet = {
    _id: toEnhetId(enhet),
    _type: "enhet",
    navn: enhet.navn ?? orThrow("'navn' is missing from enhet"),
    nummer: {
      _type: "slug",
      current: enhet.enhetNr ?? orThrow("'enhetNr' is missing from enhet"),
    },
    type: toType(enhet.type),
    status: toStatus(enhet.status),
  };

  if (fylke) {
    sanityEnhet.fylke = {
      _type: "reference",
      _ref: toEnhetId(fylke),
      _key: fylke.enhetNr!,
    };
  }

  return sanityEnhet;
}

function toEnhetId(enhet: RsEnhet) {
  return `enhet.${enhet.type!.toLocaleLowerCase()}.${enhet.enhetNr!}`;
}

function orThrow(message: string): never {
  throw new Error(message);
}

function toType(type?: string) {
  switch (type) {
    case "FYLKE":
    case "LOKAL":
    case "ALS":
      return capitalize(type);
    default:
      throw new Error(`Unexpected type '${type}'`);
  }
}

function toStatus(status?: string) {
  switch (status) {
    case "Aktiv":
    case "Nedlagt":
    case "Under utvikling":
    case "Under avvikling":
      return status;
    default:
      throw new Error(`Unexpected status '${status}'`);
  }
}

function capitalize(s: string) {
  return s[0].toLocaleUpperCase() + s.slice(1).toLocaleLowerCase();
}
