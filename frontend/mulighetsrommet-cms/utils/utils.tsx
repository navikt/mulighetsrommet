const TILTAK_GRUPPE_PROD = [
  "d03363e0-7d46-411b-aec4-fb9449e30eb8", // Arbeidsforberedende trening
  "eadeb22c-bd89-4298-a5c2-145f112f8e7d", // Arbeidsmarkedsopplæring (gruppe)
  "29c3d3cb-ffbf-4c22-8ffc-fea5d7f6c822", // Arbeidsrettet rehabilitering
  "f9618e97-4510-49e2-b748-29cae84d9019", // Avklaring
  "3526de0d-ad4c-4b81-b072-a13b3a4b4ed3", // Digitalt oppfølgingstiltak
  "73276955-0225-4b1d-8548-b67924aebd4b", // Fag- og yrkesopplæring (gruppe)
  "31e72dd8-ad05-4e81-a7f9-fd4c8f295864", // Jobbklubb
  "5ac48c03-1f4c-4d4b-b862-050caca92080", // Oppfølging
  "02509279-0a0f-4bd6-b506-f40111e4ba14", // Varig tilrettelagt arbeid i skjermet virksomhet
];

const TILTAK_GRUPPE_DEV = [
  "d03363e0-7d46-411b-aec4-fb9449e30eb8", // Arbeidsforberedende trening
  "8d0d7334-0ef4-4b8f-941e-4b41309163e9", // Arbeidsmarkedsopplæring (gruppe)
  "29c3d3cb-ffbf-4c22-8ffc-fea5d7f6c822", // Arbeidsrettet rehabilitering
  "f9618e97-4510-49e2-b748-29cae84d9019", // Avklaring
  "3526de0d-ad4c-4b81-b072-a13b3a4b4ed3", // Digitalt oppfølgingstiltak
  "7f353dcd-37c2-42f8-bab6-ac2a60669839", // Fag- og yrkesopplæring (gruppe)
  "31e72dd8-ad05-4e81-a7f9-fd4c8f295864", // Jobbklubb
  "9891f5ec-2648-436b-ab31-b46260f3b9e6", // Oppfølging
  "02509279-0a0f-4bd6-b506-f40111e4ba14", // Varig tilrettelagt arbeid i skjermet virksomhet
];

const TILTAK_ENKELTPLASS_PROD = [
  "bab45555-4631-4e5e-9a17-365fc7b335de", // Arbeidstrening
  "6f46bd0b-c9a7-4b03-bd16-e51a8f80f88d", // Fag- og yrkesopplæring eller fagskole (enkeltplass)
  "4457d760-81a4-4c16-8ab3-64c72d424db2", // Høyere utdanning
  "2ba9c085-3780-420a-a5d5-820788c74d29", // Inkluderingstilskudd
  "5328120c-028b-4ede-8250-ebf22536b021", // Mentor
  "d1521b2f-589b-4101-ae11-bd555314b905", // Midlertidig lønnstilskudd
  "18ff4bef-f62e-444a-920f-e30bde5c3950", // Tilskudd til sommerjobb
  "9911dbcb-b67f-408f-9c0c-4d7f67a863d8", // Varig lønnstilskudd
  "661e79e6-721b-452c-a6d4-8c71493f15e3", // Varig tilrettelagt arbeid i ordinær virksomhet
];

const TILTAK_ENKELTPLASS_DEV = [
  "e8406a67-fabe-4da6-804c-c77a33aaf67d", // Arbeidstrening
  "222a0065-9777-4e09-b2cf-4f48759f86e3", // Fag- og yrkesopplæring eller fagskole (enkeltplass)
  "7e159ff3-ff29-49ea-a48d-04383f6317ca", // Høyere utdanning
  "828f80b1-7d75-40dd-84b7-09e694e5c565", // Inkluderingstilskudd
  "ad998fc6-310e-45d4-a056-57732fed87b4", // Mentor
  "a97fd87c-d7c1-49af-b3fb-cf5e5c10522a", // Midlertidig lønnstilskudd
  "69fcedf9-8f85-498d-ae9e-fcdcf4931e2b", // Tilskudd til sommerjobb
  "6de22004-9fb8-4c84-9b75-dc8132a78cd2", // Varig lønnstilskudd
  "8d8abebd-3617-494a-a687-d44810e0a7ee", // Varig tilrettelagt arbeid i ordinær virksomhet
];

const TILTAK_EGEN_REGI_PROD = [
  "9fbf9feb-aa4c-4e3c-bc0e-edee0ab957ad", // Arbeid med støtte
  "d322b7e7-0c68-44d6-a325-b12d71af63c6", // IPS (individuell jobbstøtte)
  "f3faa5f2-ee9b-42da-9368-4749668144c0", // IPS Ung
];

const TILTAK_EGEN_REGI_DEV = [
  "c6a0654a-99d9-45f5-ab00-44ed7f4e1e55", // Arbeid med støtte
  "ec50a16f-633b-4ffe-99b1-512dc7fcaa2c", // IPS (individuell jobbstøtte)
  "df6354e0-5de8-4b06-9b72-74f972710cf9", // IPS Ung
];

const TILTAK_ENKELTPLASS_ANSKAFFET_PROD = [
  "6c4f372f-9631-4916-b7c1-549c17239d78", // Arbeidsmarkedsopplæring (enkeltplass)
];

const TILTAK_ENKELTPLASS_ANSKAFFET_DEV = [
  "bbb8d042-b30e-4e4a-8cd0-210019b19de3", // Arbeidsmarkedsopplæring (enkeltplass)
];

export const TILTAK_ADMINISTRERES_I_SANITY = [
  ...TILTAK_ENKELTPLASS_DEV,
  ...TILTAK_ENKELTPLASS_PROD,
  ...TILTAK_EGEN_REGI_DEV,
  ...TILTAK_EGEN_REGI_PROD,
  ...TILTAK_ENKELTPLASS_ANSKAFFET_DEV,
  ...TILTAK_ENKELTPLASS_ANSKAFFET_PROD,
];

export function isTiltakGruppe(tiltakstypeRef?: string) {
  if (!tiltakstypeRef) {
    return false;
  }
  return [...TILTAK_GRUPPE_DEV, ...TILTAK_GRUPPE_PROD].includes(tiltakstypeRef);
}

export function isTiltakEnkeltplass(tiltakstypeRef?: string) {
  if (!tiltakstypeRef) {
    return false;
  }
  return [...TILTAK_ENKELTPLASS_DEV, ...TILTAK_ENKELTPLASS_PROD].includes(tiltakstypeRef);
}

export function isTiltakEgenRegi(tiltakstypeRef?: string) {
  if (!tiltakstypeRef) {
    return false;
  }
  return [...TILTAK_EGEN_REGI_DEV, ...TILTAK_EGEN_REGI_PROD].includes(tiltakstypeRef);
}

export function isTiltakEnkeltplassAnskaffet(tiltakstypeRef?: string) {
  if (!tiltakstypeRef) {
    return false;
  }
  return [...TILTAK_ENKELTPLASS_ANSKAFFET_DEV, ...TILTAK_ENKELTPLASS_ANSKAFFET_PROD].includes(
    tiltakstypeRef,
  );
}

export const hasDuplicates = <T,>(arr: T[]): boolean => {
  return new Set(arr).size < arr.length;
};
