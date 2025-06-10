import { NavEnhet, NavEnhetType } from "@mr/api-client-v2";
import { SelectOption } from "@mr/frontend-common/components/SokeSelect";
import { MultiValue } from "react-select";

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

const spesialEnheter = [NavEnhetType.KO, NavEnhetType.ARK];
export function getSpesialUnderenheterAsSelectOptions(
  navRegioner: (string | undefined)[],
  enheter: NavEnhet[],
) {
  return getUnderenheterAsSelectOptionsBy(navRegioner, enheter, (enhet) =>
    spesialEnheter.includes(enhet.type),
  );
}

export enum NavEnhetFilterType {
  LOKAL = "KONTORER",
  ANDRE = "ANDRE",
}

export function chooseAllUnderenheterBy(
  selectedOptions: MultiValue<SelectOption<string>>,
  enheter: NavEnhet[],
  filterType: NavEnhetFilterType,
): string[] {
  const regioner = selectedOptions?.map((option) => option.value);
  switch (filterType) {
    case NavEnhetFilterType.LOKAL: {
      return getLokaleUnderenheterAsSelectOptions(regioner, enheter).map((option) => option.value);
    }
    case NavEnhetFilterType.ANDRE:
    default: {
      return getSpesialUnderenheterAsSelectOptions(regioner, enheter).map((option) => option.value);
    }
  }
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
