import { Innsatsgruppe, VeilederflateInnsatsgruppe } from 'mulighetsrommet-api-client';

export const mockInnsatsgrupper: VeilederflateInnsatsgruppe[] = [
  {
    sanityId: '642a12cf-f32e-42a5-a079-0601b7a14ee8',
    beskrivelse: 'Standardinnsats',
    nokkel: Innsatsgruppe.STANDARD_INNSATS,
    tittel: VeilederflateInnsatsgruppe.tittel.STANDARD_INNSATS,
    order: 0,
  },
  {
    sanityId: '48a20a99-11d7-42ec-ba92-2245b7d88fa7',
    beskrivelse: 'Situasjonsbestemt innsats',
    nokkel: Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
    tittel: VeilederflateInnsatsgruppe.tittel.SITUASJONSBESTEMT_INNSATS,
    order: 1,
  },
  {
    sanityId: '8dcfe56e-0018-48dd-a9f5-817f6aec0b0d',
    beskrivelse: 'Spesielt tilpasset innsats ',
    nokkel: Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
    tittel: VeilederflateInnsatsgruppe.tittel.SPESIELT_TILPASSET_INNSATS,
    order: 2,
  },
  {
    sanityId: '4193fdbe-78db-429b-9165-45abd5b3a224',
    beskrivelse: 'Varig tilpasset innsats',
    nokkel: Innsatsgruppe.VARIG_TILPASSET_INNSATS,
    tittel: VeilederflateInnsatsgruppe.tittel.VARIG_TILPASSET_INNSATS,
    order: 3,
  },
];
