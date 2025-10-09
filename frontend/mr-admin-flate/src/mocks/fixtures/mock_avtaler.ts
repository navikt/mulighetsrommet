import { mockArrangorer } from "./mock_arrangorer";
import { mockEnheter } from "./mock_enheter";
import { mockTiltakstyper } from "./mock_tiltakstyper";
import {
  ArenaMigreringOpphav,
  AvtaleDto,
  AvtaleStatusType,
  AvtaleTiltakstype,
  Avtaletype,
  DataElementStatusVariant,
  OpsjonsmodellType,
  PrismodellType,
  TiltakstypeDto,
} from "@tiltaksadministrasjon/api-client";

export const mockAvtaler: AvtaleDto[] = [
  {
    id: "d1f163b7-1a41-4547-af16-03fd4492b7ba",
    tiltakstype: getAvtaleTiltakstype(mockTiltakstyper.AVKLARAG),
    navn: "Testtiltak Varig",
    administratorer: [
      {
        navIdent: "B123456",
        navn: "Bertil Bengtson",
      },
    ],
    avtalenummer: "2021#10579",
    opphav: ArenaMigreringOpphav.TILTAKSADMINISTRASJON,
    sakarkivNummer: "2020/1234",
    beskrivelse: null,
    faneinnhold: null,
    arrangor: {
      ...mockArrangorer.data[0],
      slettet: false,
      underenheter: (mockArrangorer.data[0].underenheter || []).map((v) => ({
        id: v.id,
        organisasjonsnummer: v.organisasjonsnummer,
        navn: v.navn,
        slettet: false,
        kontaktpersoner: [],
      })),
      kontaktpersoner: [
        {
          id: "d136d6a4-c812-4d28-81db-b688187e4e32",
          navn: "Ole Kjetil Martinsen",
          epost: "ole.kjetil.martinsen@arrangor.no",
          telefon: "90123456",
          beskrivelse: "Direktør",
        },
      ],
    },
    startDato: "2021-08-02",
    sluttDato: "2023-08-01",
    avtaletype: Avtaletype.RAMMEAVTALE,
    status: {
      type: AvtaleStatusType.AKTIV,
      status: { value: "Aktiv", variant: DataElementStatusVariant.SUCCESS, description: null },
    },
    arenaAnsvarligEnhet: mockEnheter._0300,
    kontorstruktur: [
      { region: mockEnheter._0300, kontorer: [mockEnheter._0313, mockEnheter._0318] },
      {
        region: mockEnheter._0400,
        kontorer: [mockEnheter._0425, mockEnheter._0402, mockEnheter._0415],
      },
    ],
    personopplysninger: [],
    personvernBekreftet: false,
    amoKategorisering: null,
    opsjonsmodell: {
      type: OpsjonsmodellType.TO_PLUSS_EN_PLUSS_EN_PLUSS_EN,
      opsjonMaksVarighet: "2026-08-02",
      customOpsjonsmodellNavn: null,
    },
    opsjonerRegistrert: [],
    utdanningslop: null,
    prismodell: {
      type: PrismodellType.ANNEN_AVTALT_PRIS,
      navn: "Annen avtalt pris",
      beskrivelse: [],
      satser: null,
      prisbetingelser: `Nye priser fra 21.03.23, gamle priser i parentes

        10 deltakere:
        Teori en uke: 31 239,- (30 329,-)                     Praksis en uke: 26 018,- (25 260,-)                      Kombinasjon en uke: 28 396,- (27 569,-)

        15 deltakere:
        Teori en uke: 40 549,- (39 368,-)                    Praksis en uke: 36 855,- (35 782,-)                      Kombinasjon en uke: 33 780,- (32 796,-)

        20 deltakere:
        Teori en uke: 56 771,- (55 117,-)                     Praksis en uke: 45 695,- (44 364,-)                       Kombinasjon en uke: 47 344,- (45 965,-)`,
    },
  },
  {
    id: "d1f163b7-1a41-4547-af16-03fd4492b7bc",
    tiltakstype: getAvtaleTiltakstype(mockTiltakstyper.ARBFORB),
    navn: "Avtale hos ÅMLI KOMMUNE SAMFUNNSAVDELINGA",
    avtalenummer: "2021#10579",
    arrangor: {
      ...mockArrangorer.data[0],
      slettet: false,
      kontaktpersoner: [],
      underenheter: (mockArrangorer.data[0].underenheter || []).map((v) => ({
        id: v.id,
        organisasjonsnummer: v.organisasjonsnummer,
        navn: v.navn,
        slettet: false,
        kontaktpersoner: [],
      })),
    },
    opphav: ArenaMigreringOpphav.TILTAKSADMINISTRASJON,
    sakarkivNummer: "2020/1234",
    beskrivelse: null,
    faneinnhold: null,
    administratorer: [
      {
        navIdent: "B123456",
        navn: "Bertil Bengtson",
      },
    ],
    startDato: "2021-08-02",
    sluttDato: "2026-08-01",
    avtaletype: Avtaletype.RAMMEAVTALE,
    status: {
      type: AvtaleStatusType.AVBRUTT,
      status: {
        value: "Avbrutt",
        variant: DataElementStatusVariant.SUCCESS,
        description:
          "Denne avtalen ble avbrutt pga av noe som ikke var listen opp i listen over mulige årsaker.",
      },
    },
    arenaAnsvarligEnhet: mockEnheter._0400,
    kontorstruktur: [
      { region: mockEnheter._0400, kontorer: [mockEnheter._0415, mockEnheter._0402] },
    ],
    personopplysninger: [],
    personvernBekreftet: false,
    amoKategorisering: null,
    opsjonsmodell: {
      type: OpsjonsmodellType.TO_PLUSS_EN,
      opsjonMaksVarighet: "2026-08-01",
      customOpsjonsmodellNavn: null,
    },
    opsjonerRegistrert: [],
    utdanningslop: null,
    prismodell: {
      type: PrismodellType.ANNEN_AVTALT_PRIS,
      navn: "Annen avtalt pris",
      beskrivelse: [],
      satser: null,
      prisbetingelser: "Maskert prisbetingelser",
    },
  },
  {
    id: "6374b285-989d-4f78-a59e-29481b64ba92",
    opphav: ArenaMigreringOpphav.TILTAKSADMINISTRASJON,
    sakarkivNummer: "2020/1234",
    beskrivelse: null,
    faneinnhold: null,
    administratorer: [
      {
        navIdent: "B815493",
        navn: "Test Testesen",
      },
    ],
    tiltakstype: getAvtaleTiltakstype(mockTiltakstyper.INDOPPFAG),
    navn: "Avtale hos Åna Fengsel",
    avtalenummer: "2020#4929",
    arrangor: {
      ...mockArrangorer.data[0],
      slettet: false,
      kontaktpersoner: [],
      underenheter: (mockArrangorer.data[0].underenheter || []).map((v) => ({
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
    status: {
      type: AvtaleStatusType.AKTIV,
      status: { value: "Aktiv", variant: DataElementStatusVariant.SUCCESS, description: null },
    },
    arenaAnsvarligEnhet: mockEnheter._0313,
    kontorstruktur: [
      { region: mockEnheter._0400, kontorer: [mockEnheter._0415, mockEnheter._0402] },
      { region: mockEnheter._0300, kontorer: [mockEnheter._0313, mockEnheter._0318] },
    ],
    personopplysninger: [],
    personvernBekreftet: false,
    amoKategorisering: null,
    opsjonsmodell: {
      type: OpsjonsmodellType.TO_PLUSS_EN,
      opsjonMaksVarighet: "2025-06-30",
      customOpsjonsmodellNavn: null,
    },
    opsjonerRegistrert: [],
    utdanningslop: null,
    prismodell: {
      type: PrismodellType.ANNEN_AVTALT_PRIS,
      navn: "Annen avtalt pris",
      beskrivelse: [],
      satser: null,
      prisbetingelser: "Maskert prisbetingelser",
    },
  },
  {
    id: "6374b285-989d-4f78-a59e-29481b64ba93",
    opphav: ArenaMigreringOpphav.TILTAKSADMINISTRASJON,
    beskrivelse: null,
    faneinnhold: null,
    administratorer: [
      {
        navIdent: "B123456",
        navn: "Bertil Bengtson",
      },
    ],
    tiltakstype: getAvtaleTiltakstype(mockTiltakstyper.GRUFAGYRKE),
    navn: "Avtale hos Kulinarisk akademi",
    avtalenummer: "2020#4929",
    sakarkivNummer: "24/12345",
    arrangor: {
      ...mockArrangorer.data[0],
      slettet: false,
      kontaktpersoner: [],
      underenheter: (mockArrangorer.data[0].underenheter || []).map((v) => ({
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
    status: {
      type: AvtaleStatusType.AKTIV,
      status: { value: "Aktiv", variant: DataElementStatusVariant.SUCCESS, description: null },
    },
    arenaAnsvarligEnhet: mockEnheter._0313,
    kontorstruktur: [
      { region: mockEnheter._0400, kontorer: [mockEnheter._0415, mockEnheter._0402] },
      { region: mockEnheter._0300, kontorer: [mockEnheter._0313, mockEnheter._0318] },
    ],
    personopplysninger: [],
    personvernBekreftet: false,
    amoKategorisering: null,
    opsjonsmodell: {
      type: OpsjonsmodellType.ANNET,
      opsjonMaksVarighet: "2021-06-30",
      customOpsjonsmodellNavn: "1 år + 6 mnd",
    },
    opsjonerRegistrert: [],
    utdanningslop: null,
    prismodell: {
      type: PrismodellType.ANNEN_AVTALT_PRIS,
      navn: "Annen avtalt pris",
      beskrivelse: [],
      satser: null,
      prisbetingelser: "Maskert prisbetingelser",
    },
  },
];

function getAvtaleTiltakstype(dto: TiltakstypeDto): AvtaleTiltakstype {
  if (!dto.tiltakskode) {
    throw new Error("Tiltakskode mangler");
  }

  return {
    id: dto.id,
    navn: dto.navn,
    tiltakskode: dto.tiltakskode,
  };
}
