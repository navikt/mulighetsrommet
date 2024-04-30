// TODO Hente type fra openApi
interface NusData {
  value: string;
  label: string;
  children: { value: string; label: string }[];
}
[];

const ikkeVgsNivaa = [
  {
    label: "Allmenne fag",
    value: "Allmenne fag",
  },
  {
    label: "Humanistiske og estetiske fag",
    value: "Humanistiske og estetiske fag",
  },
  {
    label: "Lærerutdanninger og utdanninger i pedagogikk",
    value: "Lærerutdanninger og utdanninger i pedagogikk",
  },
  {
    label: "Samfunnsfag og juridiske fag",
    value: "Samfunnsfag og juridiske fag",
  },
  {
    label: "Naturvitenskapelige fag, håndverksfag og tekniske fag",
    value: "Naturvitenskapelige fag, håndverksfag og tekniske fag",
  },

  {
    label: "Helse-, sosial- og idrettsfag",
    value: "Helse-, sosial- og idrettsfag",
  },
  {
    label: "Primærnæringsfag",
    value: "Primærnæringsfag",
  },
  {
    label: "Samferdsels- og sikkerhetsfag og andre servicefag",
    value: "Samferdsels- og sikkerhetsfag og andre servicefag",
  },
  {
    label: "Uoppgitt fagfelt",
    value: "Uoppgitt fagfelt",
  },
];

const vgsNivaa = [
  {
    label: "Allmenne fag",
    value: "Allmenne fag",
  },
  {
    label: "Humanistiske og estetiske fag",
    value: "Humanistiske og estetiske fag",
  },
  {
    label: "Lærerutdanninger og utdanninger i pedagogikk",
    value: "Lærerutdanninger og utdanninger i pedagogikk",
  },
  {
    label: "Samfunnsfag og juridiske fag",
    value: "Samfunnsfag og juridiske fag",
  },
  {
    label: "Naturvitenskapelige fag, håndverksfag og tekniske fag",
    value: "Naturvitenskapelige fag, håndverksfag og tekniske fag",
  },
  {
    label: "Elektro",
    value: "Elektro",
  },
  {
    label: "Mekaniske fag",
    value: "Mekaniske fag",
  },
  {
    label: "Bygg og anlegg",
    value: "Bygg og anlegg",
  },
  {
    label: "Helse-, sosial- og idrettsfag",
    value: "Helse-, sosial- og idrettsfag",
  },
  {
    label: "Primærnæringsfag",
    value: "Primærnæringsfag",
  },
  {
    label: "Samferdsels- og sikkerhetsfag og andre servicefag",
    value: "Samferdsels- og sikkerhetsfag og andre servicefag",
  },
  {
    label: "Uoppgitt fagfelt",
    value: "Uoppgitt fagfelt",
  },
];

export const mockNusData: NusData[] = [
  {
    value: "Ungdomsskoleutdanning",
    label: "Ungdomsskoleutdanning",
    children: ikkeVgsNivaa,
  },
  {
    value: "Videregående, grunnutdanning",
    label: "Videregående, grunnutdanning",
    children: vgsNivaa,
  },
  {
    value: "Videregående, avsluttende utdanning",
    label: "Videregående, avsluttende utdanning",
    children: vgsNivaa,
  },
];
