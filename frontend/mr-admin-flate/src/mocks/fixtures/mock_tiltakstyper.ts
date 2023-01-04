import { PaginertTiltakstype, Tiltakskode } from "mulighetsrommet-api-client";

export const mockTiltakstyper: PaginertTiltakstype = {
  pagination: {
    totalCount: 104,
    currentPage: 1,
    pageSize: 50,
  },
  data: [
    {
      id: "afb69ca8-ddff-45be-9fd0-8f968519468d",
      navn: "Arbeid med Bistand (AB)",
      arenaKode: Tiltakskode.ABIST,
    },
    {
      id: "a7d63fb0-4366-412c-84b7-7c15518ee36c",
      navn: "Arbeid med bistand A oppfølging",
      arenaKode: Tiltakskode.ABOPPF,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3caf",
      navn: "Arbeid med bistand B",
      arenaKode: Tiltakskode.ABTBOPPF,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3ca1",
      navn: "Arbeid med bistand A utvidet oppfølging",
      arenaKode: Tiltakskode.ABUOPPF,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3ca2",
      navn: "AMB Avklaring (fase 1)",
      arenaKode: Tiltakskode.AMBF1,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3ca3",
      navn: "Kvalifisering i arbeidsmarkedsbedrift",
      arenaKode: Tiltakskode.AMBF2,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3ca4",
      navn: "Tilrettelagt arbeid i arbeidsmarkedsbedrift",
      arenaKode: Tiltakskode.AMBF3,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3ca5",
      navn: "Arbeidsmarkedsopplæring (AMO)",
      arenaKode: Tiltakskode.AMO,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3caf6",
      navn: "Arbeidsmarkedsopplæring (AMO) i bedrift",
      arenaKode: Tiltakskode.AMOB,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3ca7",
      navn: "Arbeidsmarkedsopplæring (AMO) enkeltplass",
      arenaKode: Tiltakskode.AMOE,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3ca8",
      navn: "Arbeidsmarkedsopplæring (AMO) yrkeshemmede",
      arenaKode: Tiltakskode.AMOY,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3ca9",
      navn: "Annen utdanning",
      arenaKode: Tiltakskode.ANNUTDANN,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c10",
      navn: "Arbeidsrettet rehabilitering (døgn) - sykmeldt arbeidstaker",
      arenaKode: Tiltakskode.ARBDOGNSM,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c14",
      navn: "Arbeidsforberedende trening (AFT)",
      arenaKode: Tiltakskode.ARBFORB,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c15",
      navn: "Arbeidsrettet rehabilitering (dag) - sykmeldt arbeidstaker",
      arenaKode: Tiltakskode.ARBRDAGSM,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c16",
      navn: "Arbeidsrettet rehabilitering (døgn)",
      arenaKode: Tiltakskode.ARBRRDOGN,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c17",
      navn: "Arbeidsrettet rehabilitering",
      arenaKode: Tiltakskode.ARBRRHBAG,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c18",
      navn: "Arbeidsrettet rehabilitering - sykmeldt arbeidstaker",
      arenaKode: Tiltakskode.ARBRRHBSM,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c19",
      navn: "Arbeidsrettet rehabilitering (dag)",
      arenaKode: Tiltakskode.ARBRRHDAG,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c20",
      navn: "Arbeidstrening",
      arenaKode: Tiltakskode.ARBTREN,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c21",
      navn: "Arbeidssamvirke (ASV)",
      arenaKode: Tiltakskode.ASV,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c22",
      navn: "Arbeidstreningsgrupper",
      arenaKode: Tiltakskode.ATG,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c23",
      navn: "Avklaring",
      arenaKode: Tiltakskode.AVKLARAG,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c24",
      navn: "Avklaring av kortere varighet",
      arenaKode: Tiltakskode.AVKLARKV,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c25",
      navn: "Avklaring - sykmeldt arbeidstaker",
      arenaKode: Tiltakskode.AVKLARSP,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c26",
      navn: "Avklaring i skjermet virksomhet",
      arenaKode: Tiltakskode.AVKLARSV,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c27",
      navn: "Avklaring",
      arenaKode: Tiltakskode.AVKLARUS,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c28",
      navn: "Bedriftsintern attføring",
      arenaKode: Tiltakskode.BIA,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c29",
      navn: "Bedriftsintern opplæring (BIO)",
      arenaKode: Tiltakskode.BIO,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c30",
      navn: "Brevkurs",
      arenaKode: Tiltakskode.BREVKURS,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c31",
      navn: "Digitalt oppfølgingstiltak for arbeidsledige (jobbklubb)",
      arenaKode: Tiltakskode.DIGIOPPARB,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c32",
      navn: "Diverse tiltak",
      arenaKode: Tiltakskode.DIVTILT,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c33",
      navn: "Ekspertbistand",
      arenaKode: Tiltakskode.EKSPEBIST,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c34",
      navn: "Enkeltplass AMO",
      arenaKode: Tiltakskode.ENKELAMO,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c35",
      navn: "Enkeltplass Fag- og yrkesopplæring VGS og høyere yrkesfaglig utdanning",
      arenaKode: Tiltakskode.ENKFAGYRKE,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c36",
      navn: "Egenetablering",
      arenaKode: Tiltakskode.ETAB,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c37",
      navn: "Fleksibel jobb - lønnstilskudd av lengre varighet",
      arenaKode: Tiltakskode.FLEKSJOBB,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c38",
      navn: "Forsøk AMO enkeltplass",
      arenaKode: Tiltakskode.FORSAMOENK,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c39",
      navn: "Forsøk AMO gruppe",
      arenaKode: Tiltakskode.FORSAMOGRU,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c40",
      navn: "Forsøk fag- og yrkesopplæring enkeltplass",
      arenaKode: Tiltakskode.FORSFAGENK,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c41",
      navn: "Forsøk fag- og yrkesopplæring gruppe",
      arenaKode: Tiltakskode.FORSFAGGRU,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c42",
      navn: "Forsøk høyere utdanning",
      arenaKode: Tiltakskode.FORSHOYUTD,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c43",
      navn: "Funksjonsassistanse",
      arenaKode: Tiltakskode.FUNKSJASS,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c44",
      navn: "Gruppe Fag- og yrkesopplæring VGS og høyere yrkesfaglig utdanning",
      arenaKode: Tiltakskode.GRUFAGYRKE,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c45",
      navn: "Grunnskole",
      arenaKode: Tiltakskode.GRUNNSKOLE,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c46",
      navn: "Gruppe AMO",
      arenaKode: Tiltakskode.GRUPPEAMO,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c47",
      navn: "Høyere utdanning",
      arenaKode: Tiltakskode.HOYEREUTD,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c48",
      navn: "Høyskole/Universitet",
      arenaKode: Tiltakskode.HOYSKOLE,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c49",
      navn: "Individuell jobbstøtte (IPS)",
      arenaKode: Tiltakskode.INDJOBSTOT,
    },
    {
      id: "186df85f-c773-4f34-8904-1983787a3c50",
      navn: "Oppfølging",
      arenaKode: Tiltakskode.INDOPPFAG,
    },
  ],
};
