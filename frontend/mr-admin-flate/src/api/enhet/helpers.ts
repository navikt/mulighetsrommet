import { ArenaNavEnhet, NavEnhetDto, NavEnhetType } from "@mr/api-client-v2";

export function getDisplayName(enhet: NavEnhetDto | ArenaNavEnhet) {
  const { enhetsnummer, navn } = enhet;
  return navn ? `${enhetsnummer} ${navn}` : enhetsnummer;
}

function getUnderenheterAsSelectOptionsBy(
  navRegioner: (string | undefined)[],
  enheter: NavEnhetDto[],
  predicate: (item: NavEnhetDto) => boolean,
) {
  return enheter
    .filter((enhet) => {
      return (
        enhet.overordnetEnhet != null &&
        navRegioner.includes(enhet.overordnetEnhet) &&
        predicate(enhet)
      );
    })
    .map((enhet) => ({
      label: enhet.navn,
      value: enhet.enhetsnummer,
    }));
}

export function getLokaleUnderenheterAsSelectOptions(
  navRegioner: (string | undefined)[],
  enheter: NavEnhetDto[],
) {
  return getUnderenheterAsSelectOptionsBy(
    navRegioner,
    enheter,
    (enhet) => enhet.type === NavEnhetType.LOKAL,
  );
}

const andreEnheter = [NavEnhetType.KO, NavEnhetType.ARK];

export function getAndreUnderenheterAsSelectOptions(
  navRegioner: (string | undefined)[],
  enheter: NavEnhetDto[],
) {
  return getUnderenheterAsSelectOptionsBy(navRegioner, enheter, (enhet) =>
    andreEnheter.includes(enhet.type),
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
