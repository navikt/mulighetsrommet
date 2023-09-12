import { Innsatsgruppe, SanityInnsatsgruppe } from 'mulighetsrommet-api-client';

export const mockInnsatsgrupper: SanityInnsatsgruppe[] = [
  {
    tittel: SanityInnsatsgruppe.tittel.STANDARD_INNSATS,
    beskrivelse: 'Standardinnsats',
    nokkel: Innsatsgruppe.STANDARD_INNSATS,
    _id: '642a12cf-f32e-42a5-a079-0601b7a14ee8',
  },
  {
    beskrivelse: 'Situasjonsbestemt innsats',
    _id: '48a20a99-11d7-42ec-ba92-2245b7d88fa7',
    nokkel: Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
    tittel: SanityInnsatsgruppe.tittel.SITUASJONSBESTEMT_INNSATS,
  },
  {
    _id: '8dcfe56e-0018-48dd-a9f5-817f6aec0b0d',
    nokkel: Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
    beskrivelse: 'Spesielt tilpasset innsats ',
    tittel: SanityInnsatsgruppe.tittel.SPESIELT_TILPASSET_INNSATS,
  },
  {
    nokkel: Innsatsgruppe.VARIG_TILPASSET_INNSATS,
    beskrivelse: 'Varig tilpasset innsats',
    _id: '4193fdbe-78db-429b-9165-45abd5b3a224',
    tittel: SanityInnsatsgruppe.tittel.VARIG_TILPASSET_INNSATS,
  },
];
