import {
  ArenaNavEnhet,
  NavEnhetDto,
  NavEnhetType,
  NavRegionDto,
  NavRegionUnderenhetDto,
} from "@tiltaksadministrasjon/api-client";

export function getDisplayName(enhet: NavEnhetDto | ArenaNavEnhet) {
  const { enhetsnummer, navn } = enhet;
  return navn ? `${enhetsnummer} ${navn}` : enhetsnummer;
}

export function getLokaleUnderenheterAsSelectOptions(
  valgteRegioner: (string | undefined)[],
  regioner: NavRegionDto[],
) {
  return getUnderenheterAsSelectOptionsBy(
    valgteRegioner,
    regioner,
    (enhet) => enhet.erStandardvalg,
  );
}

export function getAndreUnderenheterAsSelectOptions(
  valgteRegioner: (string | undefined)[],
  regioner: NavRegionDto[],
) {
  return getUnderenheterAsSelectOptionsBy(
    valgteRegioner,
    regioner,
    (enhet) => !enhet.erStandardvalg,
  );
}

function getUnderenheterAsSelectOptionsBy(
  valgteRegioner: (string | undefined)[],
  regioner: NavRegionDto[],
  predicate: (item: NavRegionUnderenhetDto) => boolean,
) {
  return regioner
    .filter((region) => valgteRegioner.includes(region.enhetsnummer))
    .flatMap((region) =>
      region.enheter.filter(predicate).map((enhet) => ({
        value: enhet.enhetsnummer,
        label: enhet.navn,
      })),
    );
}

export interface TypeSplittedNavEnheter {
  navKontorEnheter: NavEnhetDto[];
  navAndreEnheter: NavEnhetDto[];
}

export function splitNavEnheterByType(navEnheter: NavEnhetDto[]): TypeSplittedNavEnheter {
  const initial = { navKontorEnheter: [], navAndreEnheter: [] };
  if (!navEnheter.length) {
    return initial;
  }
  return navEnheter.reduce<TypeSplittedNavEnheter>(
    ({ navKontorEnheter, navAndreEnheter }, currNavEnhet) => {
      if (currNavEnhet.type === NavEnhetType.LOKAL) {
        return { navKontorEnheter: [currNavEnhet, ...navKontorEnheter], navAndreEnheter };
      } else {
        return {
          navKontorEnheter,
          navAndreEnheter: [currNavEnhet, ...navAndreEnheter],
        };
      }
    },
    initial,
  );
}
