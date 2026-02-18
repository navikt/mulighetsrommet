import {
  Kontorstruktur,
  KontorstrukturKontor,
  KontorstrukturKontortype,
} from "@tiltaksadministrasjon/api-client";

export function getLokaleUnderenheterAsSelectOptions(
  valgteRegioner: (string | undefined)[],
  regioner: Kontorstruktur[],
) {
  return getUnderenheterAsSelectOptionsBy(
    valgteRegioner,
    regioner,
    (enhet) => enhet.type === KontorstrukturKontortype.LOKAL,
  );
}

export function getAndreUnderenheterAsSelectOptions(
  valgteRegioner: (string | undefined)[],
  regioner: Kontorstruktur[],
) {
  return getUnderenheterAsSelectOptionsBy(
    valgteRegioner,
    regioner,
    (enhet) => enhet.type === KontorstrukturKontortype.SPESIALENHET,
  );
}

function getUnderenheterAsSelectOptionsBy(
  valgteRegioner: (string | undefined)[],
  regioner: Kontorstruktur[],
  predicate: (item: KontorstrukturKontor) => boolean,
) {
  return regioner
    .filter(({ region }) => valgteRegioner.includes(region.enhetsnummer))
    .flatMap(({ kontorer }) =>
      kontorer.filter(predicate).map((enhet) => ({
        value: enhet.enhetsnummer,
        label: enhet.navn,
      })),
    );
}

export interface TypeSplittedNavEnheter {
  navKontorEnheter: KontorstrukturKontor[];
  navAndreEnheter: KontorstrukturKontor[];
}

export function splitNavEnheterByType(navEnheter: KontorstrukturKontor[]): TypeSplittedNavEnheter {
  const initial = { navKontorEnheter: [], navAndreEnheter: [] };
  if (!navEnheter.length) {
    return initial;
  }
  return navEnheter.reduce<TypeSplittedNavEnheter>(
    ({ navKontorEnheter, navAndreEnheter }, currNavEnhet) => {
      if (currNavEnhet.type === KontorstrukturKontortype.LOKAL) {
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
