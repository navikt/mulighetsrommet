import {
  EstimertVentetid,
  Opphav,
  PaginertTiltaksgjennomforing,
  Tiltaksgjennomforing,
  TiltaksgjennomforingOppstartstype,
  TiltaksgjennomforingStatus,
} from "mulighetsrommet-api-client";
import { mockTiltakstyper } from "./mock_tiltakstyper";
import { nikolineKontaktperson, petrusKontaktperson } from "./mock_ansatt";
import { mockEnheter } from "./mock_enheter";
import { mockArrangorer } from "./mock_arrangorer";
import { mockArrangorKontaktpersoner } from "./mock_arrangorKontaktperson";
import { mockAvtaler } from "./mock_avtaler";

export const mockTiltaksgjennomforinger: Tiltaksgjennomforing[] = [
  {
    id: "a7d63fb0-4366-412c-84b7-7c15518ee361",
    navn: "Yrkesnorsk med praksis med en veldig lang tittel som ikke er helt utenkelig at de skriver inn",
    tiltaksnummer: "123456",
    estimertVentetid: {
      verdi: 3,
      enhet: EstimertVentetid.enhet.MANED,
    },
    antallPlasser: 50,
    arrangor: {
      ...mockArrangorer.fretex.underenheter!![0],
      slettet: false,
      kontaktpersoner: [mockArrangorKontaktpersoner[0]],
    },
    avtaleId: mockAvtaler[0].id,
    tiltakstype: mockTiltakstyper.AVKLARAG,
    administratorer: [
      {
        navIdent: "B123456",
        navn: "Bertil Bengtson",
      },
    ],
    sanityId: "123",
    startDato: "2022-01-01",
    sluttDato: "2029-12-12",
    deltidsprosent: 100,
    arenaAnsvarligEnhet: mockEnheter._0313,
    navEnheter: [mockEnheter._0313, mockEnheter._0315, mockEnheter._0330],
    status: TiltaksgjennomforingStatus.GJENNOMFORES,
    oppstart: TiltaksgjennomforingOppstartstype.LOPENDE,
    opphav: Opphav.MR_ADMIN_FLATE,
    apentForInnsok: false,
    stedForGjennomforing: "Brummundal",
    kontaktpersoner: [petrusKontaktperson, nikolineKontaktperson],
    publisert: true,
    publisertForAlle: true,
    beskrivelse: "bla bla bla beskrivelse",
    faneinnhold: {
      forHvem: [
        {
          _type: "block",
          children: [
            {
              text: 'Richard had a difficult and often reactionary personality, and was defiantly Northern English in outlook. Brix said that he carried "a chip on both shoulders. I remember him talking about fucking southern bastards a lot and not wanting to come to London. He hated London intensely.Skriver noe mer fornuftig her',
              _type: "span",
            },
          ],
          markDefs: [],
        },
      ],
      pameldingOgVarighet: [
        {
          _key: "eb83652d429d",
          _type: "block",
          style: "normal",
          children: [
            {
              _key: "34b0c44b289b0",
              text: "Nulla lacinia lorem erat, ut sollicitudin ex commodo in. Maecenas ac purus at urna accumsan sagittis. Aenean ornare massa lacus, non tempor orci venenatis in.",
              _type: "span",
              marks: [],
            },
          ],
          markDefs: [],
        },
      ],
      detaljerOgInnholdInfoboks:
        "Phasellus a urna eget augue pharetra posuere. Aenean malesuada tortor eros, ut ullamcorper nisi facilisis eget. ",
      pameldingOgVarighetInfoboks:
        "Nullam lobortis massa nibh, luctus molestie lacus venenatis sed.",
    },
  },
  {
    id: "a7d63fb0-4366-412c-84b7-7c15518ee362",
    navn: "Spillbasert kvalifisering",
    tiltaksnummer: "123456",
    deltidsprosent: 100,
    arrangor: {
      ...mockArrangorer.fretex.underenheter![0],
      slettet: false,
      kontaktpersoner: mockArrangorKontaktpersoner,
    },
    tiltakstype: mockTiltakstyper.ARBFORB,
    sanityId: "1234",
    startDato: "2022-01-01",
    sluttDato: "2022-12-12",
    arenaAnsvarligEnhet: mockEnheter._0313,
    administratorer: [],
    navEnheter: [],
    status: TiltaksgjennomforingStatus.AVLYST,
    oppstart: TiltaksgjennomforingOppstartstype.LOPENDE,
    opphav: Opphav.MR_ADMIN_FLATE,
    apentForInnsok: true,
    kontaktpersoner: [],
    publisert: false,
    publisertForAlle: false,
  },
  {
    id: "a7d63fb0-4366-412c-84b7-7c15518ee363",
    navn: "AFT",
    tiltaksnummer: "654434",
    sanityId: "1234",
    deltidsprosent: 100,
    arrangor: {
      ...mockArrangorer.fretex.underenheter![0],
      slettet: false,
      kontaktpersoner: mockArrangorKontaktpersoner,
    },
    tiltakstype: mockTiltakstyper.ARBFORB,
    startDato: "2022-01-01",
    sluttDato: "2022-12-12",
    arenaAnsvarligEnhet: mockEnheter._0313,
    administratorer: [],
    navEnheter: [],
    status: TiltaksgjennomforingStatus.GJENNOMFORES,
    oppstart: TiltaksgjennomforingOppstartstype.LOPENDE,
    opphav: Opphav.MR_ADMIN_FLATE,
    apentForInnsok: true,
    kontaktpersoner: [],
    publisert: false,
    publisertForAlle: true,
  },
];

export const paginertMockTiltaksgjennomforinger: PaginertTiltaksgjennomforing = {
  pagination: {
    totalCount: 18,
    currentPage: 1,
    pageSize: 50,
    totalPages: 1,
  },
  data: mockTiltaksgjennomforinger,
};

// Bruker denne for å teste med flere tiltaksgjennomføringer lokalt, men setter den til 0 sånn
// at testene går gjennom.
const x = 0;
for (let i = 0; i < x; i++) {
  mockTiltaksgjennomforinger.push({
    id: "a7d63fb0-4366-412c-84b7-7c15518ee363",
    navn: "AFT",
    tiltaksnummer: "654434",
    sanityId: "1234",
    deltidsprosent: 100,
    arrangor: {
      ...mockArrangorer.fretex.underenheter![0],
      slettet: false,
      kontaktpersoner: mockArrangorKontaktpersoner,
    },
    tiltakstype: mockTiltakstyper.ARBFORB,
    startDato: "2022-01-01",
    sluttDato: "2022-12-12",
    arenaAnsvarligEnhet: mockEnheter._0313,
    administratorer: [],
    navEnheter: [],
    status: TiltaksgjennomforingStatus.GJENNOMFORES,
    oppstart: TiltaksgjennomforingOppstartstype.LOPENDE,
    opphav: Opphav.MR_ADMIN_FLATE,
    apentForInnsok: true,
    kontaktpersoner: [],
    publisert: false,
    publisertForAlle: true,
  });
}
