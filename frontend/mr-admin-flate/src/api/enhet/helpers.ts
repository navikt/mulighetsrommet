import { ArenaNavEnhet, NavEnhet, NavEnhetType } from "@mr/api-client-v2";
import { SelectOption } from "@mr/frontend-common/components/SokeSelect";
import { MultiValue } from "react-select";

export function getDisplayName(enhet: NavEnhet | ArenaNavEnhet) {
  const { enhetsnummer, navn } = enhet;
  return navn ? `${enhetsnummer} ${navn}` : enhetsnummer;
}

function getUnderenheterAsSelectOptionsBy(
  navRegioner: (string | undefined)[],
  enheter: NavEnhet[],
  predicate: (item: NavEnhet) => boolean,
) {
  return enheter
    .filter((enhet: NavEnhet) => {
      return (
        enhet.overordnetEnhet != null &&
        navRegioner.includes(enhet?.overordnetEnhet) &&
        predicate(enhet)
      );
    })
    .map((enhet: NavEnhet) => ({
      label: enhet.navn,
      value: enhet.enhetsnummer,
    }));
}

export function getLokaleUnderenheterAsSelectOptions(
  navRegioner: (string | undefined)[],
  enheter: NavEnhet[],
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
  enheter: NavEnhet[],
) {
  return getUnderenheterAsSelectOptionsBy(navRegioner, enheter, (enhet) =>
    andreEnheter.includes(enhet.type),
  );
}

export function velgAlleLokaleUnderenheter(
  selectedOptions: MultiValue<SelectOption<string>>,
  enheter: NavEnhet[],
): string[] {
  const regioner = selectedOptions?.map((option) => option.value);
  return getLokaleUnderenheterAsSelectOptions(regioner, enheter).map((option) => option.value);
}

export interface TypeSplittedNavEnheter {
  navKontorEnheter: NavEnhet[];
  navAndreEnheter: NavEnhet[];
}

export function splitNavEnheterByType(navEnheter: NavEnhet[]): TypeSplittedNavEnheter {
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
