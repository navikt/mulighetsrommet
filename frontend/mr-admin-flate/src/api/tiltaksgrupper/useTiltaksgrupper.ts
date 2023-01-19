export type Tiltaksgruppe = {
  id: string;
  navn: string;
  arenaKode: string;
};

export function useTiltaksgrupper(): Tiltaksgruppe[] {
  return [
    { id: "1", navn: "Arbeidsmarkedsbedrift", arenaKode: "AMD" },
    { id: "2", navn: "Arbeidspraksis", arenaKode: "ARBPRAKS" },
    { id: "3", navn: "Avklaring", arenaKode: "AVKLARING" },
    { id: "4", navn: "Bedriftsintern opplæring", arenaKode: "BIO" },
    { id: "5", navn: "Egenetablering", arenaKode: "ETAB" },
    { id: "6", navn: "Forsøk", arenaKode: "FORSØK" },
    { id: "7", navn: "Jobbskapningsprosjekter", arenaKode: "JOBBSKAP" },
    { id: "8", navn: "Lønnstilskudd", arenaKode: "LONNTILS" },
    { id: "9", navn: "Midlertidig sysselsetting", arenaKode: "MIDDSYS" },
    { id: "10", navn: "Opplæring", arenaKode: "OPPL" },
  ];
}
