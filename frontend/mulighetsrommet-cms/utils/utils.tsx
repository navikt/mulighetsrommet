export const IKKE_I_ADMINFLATE_TILTAK_PROD = [
  "18ff4bef-f62e-444a-920f-e30bde5c3950", // "Tilskudd til sommerjobb",
  "2ba9c085-3780-420a-a5d5-820788c74d29", //  Inkluderingstilskudd,
  "4457d760-81a4-4c16-8ab3-64c72d424db2", // Opplæring - Høyere utdanning",
  "5328120c-028b-4ede-8250-ebf22536b021", // "Mentor",
  "6c4f372f-9631-4916-b7c1-549c17239d78", // "Opplæring - Enkeltplass AMO",
  "9911dbcb-b67f-408f-9c0c-4d7f67a863d8", // "Varig lønnstilskudd",
  "6f46bd0b-c9a7-4b03-bd16-e51a8f80f88d", // "Opplæring - Fagskole (høyere yrkesfaglig utdanning)",
  "d1521b2f-589b-4101-ae11-bd555314b905", // "Midlertidig lønnstilskudd",
  "bab45555-4631-4e5e-9a17-365fc7b335de", // "Arbeidstrening",
  "661e79e6-721b-452c-a6d4-8c71493f15e3", // VATIAROR | Varig tilrettelagt arbeid i ordinær virksomhet
  "9fbf9feb-aa4c-4e3c-bc0e-edee0ab957ad", // Arbeid med støtte
  "f3faa5f2-ee9b-42da-9368-4749668144c0", // IPS UNG
  "d322b7e7-0c68-44d6-a325-b12d71af63c6", // IPS
];

export const IKKE_I_ADMINFLATE_TILTAK_DEV = [
  "ad998fc6-310e-45d4-a056-57732fed87b4", // "Mentor",
  "50878ad5-90d0-496d-a0d0-a53091800760", // "Opplæring - Høyere utdanning",
  "bbb8d042-b30e-4e4a-8cd0-210019b19de3", // "Opplæring - Enkeltplass AMO",
  "6de22004-9fb8-4c84-9b75-dc8132a78cd2", // "Varig lønnstilskudd",
  "a97fd87c-d7c1-49af-b3fb-cf5e5c10522a", // "Midlertidig lønnstilskudd",
  "69fcedf9-8f85-498d-ae9e-fcdcf4931e2b", // "Tilskudd til sommerjobb",
  "e8406a67-fabe-4da6-804c-c77a33aaf67d", // "Arbeidstrening",
  "8d8abebd-3617-494a-a687-d44810e0a7ee", // "Varig tilrettelagt arbeids i ordinær virksomhet",
];

export const TILTAK_I_EGEN_REGI_PROD = [
  "f3faa5f2-ee9b-42da-9368-4749668144c0", // IPS UNG
  "9fbf9feb-aa4c-4e3c-bc0e-edee0ab957ad", // Arbeid med støtte
  "d322b7e7-0c68-44d6-a325-b12d71af63c6", // IPS
];

export const isInAdminFlate = (tiltakstypeRef?: string) => {
  if (!tiltakstypeRef) {
    return false;
  }
  return (
    !IKKE_I_ADMINFLATE_TILTAK_PROD.includes(tiltakstypeRef) &&
    !IKKE_I_ADMINFLATE_TILTAK_DEV.includes(tiltakstypeRef)
  );
};

export const isEgenRegiTiltak = (tiltakstypeRef?: string) => {
  if (!tiltakstypeRef) {
    return false;
  }
  return TILTAK_I_EGEN_REGI_PROD.includes(tiltakstypeRef);
};

export const hasDuplicates = <T,>(arr: T[]): boolean => {
  return new Set(arr).size < arr.length;
};
