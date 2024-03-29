import { Avtale, Avtalestatus, Avtaletype, Opphav } from "mulighetsrommet-api-client";
import { mockEnheter } from "./mock_enheter";
import { mockTiltakstyper } from "./mock_tiltakstyper";
import { mockArrangorer } from "./mock_arrangorer";

export const mockAvtaler: Avtale[] = [
  {
    id: "d1f163b7-1a41-4547-af16-03fd4492b7ba",
    tiltakstype: mockTiltakstyper.AVKLARAG,
    navn: "Testtiltak Varig",
    administratorer: [
      {
        navIdent: "B123456",
        navn: "Bertil Bengtson",
      },
    ],
    opphav: Opphav.MR_ADMIN_FLATE,
    avtalenummer: "2021#10579",
    arrangor: {
      ...mockArrangorer.fretex,
      slettet: false,
      underenheter: mockArrangorer.fretex.underenheter!.map((v) => ({
        id: v.id,
        organisasjonsnummer: v.organisasjonsnummer,
        navn: v.navn,
        slettet: false,
        kontaktpersoner: [],
      })),
      kontaktperson: {
        id: "d136d6a4-c812-4d28-81db-b688187e4e32",
        arrangorId: "a714ca5e-857e-41a7-85d7-8be1c1d483ab",
        navn: "Ole Kjetil Martinsen",
        epost: "ole.kjetil.martinsen@arrangor.no",
        telefon: "90123456",
        beskrivelse: "Direktør",
      },
    },
    startDato: "2021-08-02",
    sluttDato: "2026-08-01",
    avtaletype: Avtaletype.FORHAANDSGODKJENT,
    avtalestatus: Avtalestatus.AKTIV,
    arenaAnsvarligEnhet: mockEnheter._0300,
    prisbetingelser: `Nye priser fra 21.03.23, gamle priser i parentes

        10 deltakere:
        Teori en uke: 31 239,- (30 329,-)                     Praksis en uke: 26 018,- (25 260,-)                      Kombinasjon en uke: 28 396,- (27 569,-)

        15 deltakere:
        Teori en uke: 40 549,- (39 368,-)                    Praksis en uke: 36 855,- (35 782,-)                      Kombinasjon en uke: 33 780,- (32 796,-)

        20 deltakere:
        Teori en uke: 56 771,- (55 117,-)                     Praksis en uke: 45 695,- (44 364,-)                       Kombinasjon en uke: 47 344,- (45 965,-)`,
    url: "https://www.websak.no",
    kontorstruktur: [
      { region: mockEnheter._0300, kontorer: [mockEnheter._0313, mockEnheter._0318] },
      {
        region: mockEnheter._0400,
        kontorer: [mockEnheter._0425, mockEnheter._0402, mockEnheter._0415],
      },
    ],
  },
  {
    id: "d1f163b7-1a41-4547-af16-03fd4492b7bc",
    tiltakstype: mockTiltakstyper.ARBFORB,
    navn: "Avtale hos ÅMLI KOMMUNE SAMFUNNSAVDELINGA",
    avtalenummer: "2021#10579",
    arrangor: {
      ...mockArrangorer.fretex,
      slettet: false,
      underenheter: mockArrangorer.fretex.underenheter!.map((v) => ({
        id: v.id,
        organisasjonsnummer: v.organisasjonsnummer,
        navn: v.navn,
        slettet: false,
        kontaktpersoner: [],
      })),
    },
    opphav: Opphav.ARENA,
    administratorer: [
      {
        navIdent: "B123456",
        navn: "Bertil Bengtson",
      },
    ],
    startDato: "2021-08-02",
    sluttDato: "2026-08-01",
    avtaletype: Avtaletype.RAMMEAVTALE,
    avtalestatus: Avtalestatus.AKTIV,
    arenaAnsvarligEnhet: mockEnheter._0400,
    prisbetingelser: "Maskert prisbetingelser",
    url: null,
    kontorstruktur: [
      { region: mockEnheter._0400, kontorer: [mockEnheter._0415, mockEnheter._0402] },
    ],
  },
  {
    id: "6374b285-989d-4f78-a59e-29481b64ba92",
    opphav: Opphav.ARENA,
    administratorer: [
      {
        navIdent: "B123456",
        navn: "Bertil Bengtson",
      },
    ],
    tiltakstype: mockTiltakstyper.INDOPPFAG,
    navn: "Avtale hos Åna Fengsel",
    avtalenummer: "2020#4929",
    url: "https://www.websak.no",
    arrangor: {
      ...mockArrangorer.fretex,
      slettet: false,
      underenheter: mockArrangorer.fretex.underenheter!.map((v) => ({
        id: v.id,
        organisasjonsnummer: v.organisasjonsnummer,
        navn: v.navn,
        slettet: false,
        kontaktpersoner: [],
      })),
    },
    startDato: "2020-07-01",
    sluttDato: "2024-06-30",
    avtaletype: Avtaletype.RAMMEAVTALE,
    avtalestatus: Avtalestatus.AKTIV,
    arenaAnsvarligEnhet: mockEnheter._0313,
    prisbetingelser: "Maskert prisbetingelser",
    kontorstruktur: [
      { region: mockEnheter._0400, kontorer: [mockEnheter._0415, mockEnheter._0402] },
      { region: mockEnheter._0300, kontorer: [mockEnheter._0313, mockEnheter._0318] },
    ],
  },
];
