import {
  Innsatsgruppe,
  TiltakskodeArena,
  VeilederflateTiltakstype,
} from "mulighetsrommet-api-client";

export const mockTiltakstyper: { [name: string]: VeilederflateTiltakstype } = {
  VTA: {
    sanityId: "02509279-0a0f-4bd6-b506-f40111e4ba14",
    regelverkLenker: [
      {
        _id: "123",
        regelverkLenkeNavn: "Regelverk",
        regelverkUrl: "https://www.google.no",
      },
      {
        _id: "1234",
        regelverkLenkeNavn: "Rundskriv",
        regelverkUrl: "https://www.google.no",
      },
    ],
    faneinnhold: {
      forHvem: [
        {
          style: "normal",
          _key: "1580e03a6704",
          markDefs: [],
          children: [
            {
              marks: [],
              text: "VTA-S retter seg mot brukere som har, eller i nær framtid ventes å få, innvilget varig uføretrygd, og som har behov for spesiell tilrettelegging og tett oppfølging.",
              _key: "7cdd3f2c996f",
              _type: "span",
            },
          ],
          _type: "block",
        },
        {
          _key: "c0449a98e753",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "",
              _key: "870241502bc3",
            },
          ],
          _type: "block",
          style: "normal",
        },
        {
          _type: "block",
          style: "normal",
          _key: "8d573b158e24",
          markDefs: [],
          children: [
            {
              _key: "2eef87760da9",
              _type: "span",
              marks: ["strong"],
              text: "For hvem: ",
            },
          ],
        },
        {
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Brukere med varig tilpasset innsats",
              _key: "5b926584b425",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
          _key: "290c509ba173",
          listItem: "bullet",
        },
        {
          level: 1,
          _type: "block",
          style: "normal",
          _key: "ce36fe796ae2",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Brukere som mottar hel eller delvis uførepensjon",
              _key: "61bc78233a19",
            },
          ],
        },
        {
          _type: "block",
          style: "normal",
          _key: "b27e7b7d85d4",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _key: "f2ad5932f78f",
              _type: "span",
              marks: [],
              text: "Andre arbeidsrettede tiltak skal være vurdert og funnet å være uaktuelle for brukeren før brukeren vurderes til  VTA\n",
            },
          ],
          level: 1,
        },
      ],
      detaljerOgInnhold: [
        {
          style: "normal",
          _key: "286a293f78d3",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Tiltaksarrangør har ansvar for å tilby den enkelte arbeidstaker tilrettelagt arbeid og kvalifisering.\n\nTiltaket skal tilby en stabil varig arbeidsplass hvor deltakere kan utvikle sin arbeidsevne og som kan bidra til økt livskvalitet.\n\nArbeidsoppgavene som tilbys må kontinuerlig evalueres og tilpasses ut fra de behov og forutsetninger arbeidstakerne har.",
              _key: "66676c28e4ed",
            },
          ],
          _type: "block",
        },
        {
          style: "normal",
          _key: "77c85a148f1a",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "",
              _key: "5f8fa0b2701a",
            },
          ],
          _type: "block",
        },
        {
          children: [
            {
              _type: "span",
              marks: [],
              text: "Varig tilrettelagt arbeid (VTA) - Nav.no",
              _key: "794c9f2a3166",
            },
          ],
          _type: "block",
          style: "normal",
          _key: "8e838d5c5cab",
          markDefs: [],
        },
      ],
    },
    delingMedBruker:
      "Vi har nå et arbeidsmarkedstiltak som jeg tenker kan passe deg godt. Det heter <tiltaksnavn> og  er et tilbud for deg som får uføretrygd. Du jobber i en skjermet bedrift med arbeidsoppgaver som er tilpasset deg. Du kan også jobbe i en ordinær bedrift.\n\nDu kan lese mer om tiltaket på www.nav.no/varig-tilrettelagt-arbeid",
    beskrivelse:
      "Varig tilrettelagt arbeid (VTA-S) skal gi brukeren arbeid med oppgaver tilpasset han eller hennes arbeidsevne, og tilby et individuelt tilpasset opplegg.",
    navn: "VTA - Varig tilrettelagt arbeid",
    innsatsgrupper: [
      Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
      Innsatsgruppe.VARIG_TILPASSET_INNSATS,
    ],
    kanKombineresMed: [],
  },
  OpplaringEnkeltplassFagOgYrke: {
    sanityId: "222a0065-9777-4e09-b2cf-4f48759f86e3",
    beskrivelse:
      "Opplæringstiltak i form av Fag- og yrkesopplæring (jf. tiltaksforskriften § 7-2 b) er opplæring som fører frem mot fag- eller svennebrev, praksisbrev eller kompetansebevis som lærekandidat. \n\nOpplæringen skal bidra til at arbeidssøkere kvalifiseres til ledige jobber. ",
    navn: "Opplæring - Enkeltplass Fag- og yrkesopplæring",
    faneinnhold: {
      forHvem: [
        {
          style: "normal",
          _key: "890591b0e2f0",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              text: "Brukere med behov for kvalifisering for å komme inn på arbeidsmarkedet",
              _key: "67a4a49ed07c",
              _type: "span",
              marks: [],
            },
          ],
          level: 1,
          _type: "block",
        },
        {
          level: 1,
          _type: "block",
          style: "normal",
          _key: "4452c393d182",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              marks: [],
              text: "Brukere få opplæringstiltak i form av fagskole fra det året de fyller 19 år",
              _key: "8d6fbc07d76d",
              _type: "span",
            },
          ],
        },
        {
          _type: "block",
          style: "normal",
          _key: "d6ca0d7bd5ea",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              marks: [],
              text: "Åpent for brukere med situasjonsbestemt, spesielt tilpasset og varig tilpasset innsats",
              _key: "92dc887ca319",
              _type: "span",
            },
          ],
          level: 1,
        },
      ],
      detaljerOgInnhold: [
        {
          style: "normal",
          _key: "5c7380e19b69",
          markDefs: [],
          children: [
            {
              marks: [],
              text: 'Enkeltplass fag- og yrkesopplæring skal registreres som "Enkeltplass Fag- og yrkesopplæring VGS og høyere yrkesfaglig utdanning" i Arena. ',
              _key: "22742af00dc3",
              _type: "span",
            },
          ],
          _type: "block",
        },
        {
          _key: "3dfe5f89b80c",
          markDefs: [],
          children: [
            {
              marks: [],
              text: "Tiltaket kan gis i inntil tre år, med mulighet for forlengelse i ett år dersom tiltaksdeltakeren av særlige grunner ikke kan gjennomføre utdanningen i løpet av tre år.",
              _key: "1de959d0dc67",
              _type: "span",
            },
          ],
          _type: "block",
          style: "normal",
        },
      ],
      pameldingOgVarighet: [
        {
          style: "normal",
          _key: "28ad9b3b6efc",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Husk å følge rutiner for anskaffelse av enkeltplasser hvis NAV skal betale for en skoleplass for en bruker.",
              _key: "716ab73fbda40",
            },
          ],
          _type: "block",
        },
      ],
    },
    innsatsgrupper: [
      Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
      Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
      Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
      Innsatsgruppe.VARIG_TILPASSET_INNSATS,
    ],
  },
  Arbeidsrettet_rehabilitering: {
    innsatsgrupper: [
      Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
      Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
      Innsatsgruppe.VARIG_TILPASSET_INNSATS,
    ],
    sanityId: "29c3d3cb-ffbf-4c22-8ffc-fea5d7f6c822",
    delingMedBruker:
      "Vi har nå et arbeidsmarkedstiltak som jeg tenker kan passe deg godt. Det heter <tiltaksnavn> og er et tilbud for deg som har helseplager eller sosiale problemer.\n\nTiltaket blir tilpasset behovet ditt ut fra mulighetene dine på arbeidsmarkedet, og kan blant annet inneholde:\n- motivasjons- og mestringsaktiviteter\n- individuelt treningsopplegg med veiledning\n- arbeidsutprøving i trygge omgivelser\n- veiledning i livsstil\n\nDu kan lese mer om kurset på www.nav.no/arbeidsrettet-rehabilitering ",
    regelverkLenker: [
      {
        _id: "123",
        regelverkLenkeNavn: "Regelverk",
        regelverkUrl: "https://www.google.no",
      },
      {
        _id: "1234",
        regelverkLenkeNavn: "Rundskriv",
        regelverkUrl: "https://www.google.no",
      },
    ],
    beskrivelse:
      "Arbeidsrettet rehabilitering skal styrke den enkeltes arbeidsevne og bidra til mestring av helserelaterte og sosiale problemer som hindrer deltakelse i arbeidslivet. Målet med tiltaket er at deltakeren skal komme ut i eller forbli i arbeid, og passer spesielt godt for sykemeldte ",
    navn: "Arbeidsrettet rehabilitering",
    faneinnhold: {
      pameldingOgVarighet: [
        {
          markDefs: [],
          children: [
            {
              text: "Innsøkingen gjøres i Arena. I Arena blir det generert et innsøkingsbrev som skal sendes til leverandør.",
              _key: "e18da464a378",
              _type: "span",
              marks: [],
            },
          ],
          _type: "block",
          style: "normal",
          _key: "ecfe0bb0af31",
        },
        {
          _type: "block",
          style: "normal",
          _key: "94dda00a0052",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "",
              _key: "b8cd64661835",
            },
          ],
        },
        {
          _type: "block",
          style: "normal",
          _key: "e4d1f3cf3e0c",
          markDefs: [],
          children: [
            {
              marks: [],
              text: "ARR kan vare i inntil 12 uker.",
              _key: "75a9b6c8cded",
              _type: "span",
            },
          ],
        },
        {
          _type: "block",
          style: "normal",
          _key: "17125bac927e",
          markDefs: [],
          children: [
            {
              _key: "fcac210d2c57",
              _type: "span",
              marks: [],
              text: "\nTiltaket gjennomføres som et heltidstilbud, og omfanget skal være individuelt tilpasset til den enkelte deltakers behov.",
            },
          ],
        },
      ],
      forHvem: [
        {
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: ["strong"],
              text: "For hvem:",
              _key: "75fe77f7d638",
            },
          ],
          _type: "block",
          style: "normal",
          _key: "7ecdb668efeb",
        },
        {
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Sykmeldte arbeidstakere",
              _key: "e071d74571d9",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
          _key: "8aec8a1db5a6",
          listItem: "bullet",
        },
        {
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              marks: [],
              text: "Brukere med nedsatt arbeidsevne",
              _key: "f869b120c717",
              _type: "span",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
          _key: "f1d45cf69a6f",
        },
        {
          _key: "cb38fa7c8c8a",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "For sykmeldte arbeidstakere er det ikke nødvendig med behovs-/arbeidsevnevurdering og 14a-vedtak.",
              _key: "0f85e50cad70",
            },
          ],
          _type: "block",
          style: "normal",
        },
      ],
      detaljerOgInnholdInfoboks:
        "Tiltaket passer særskilt godt for sykemeldte med arbeidsforhold og bør benyttes tidlig i sykdomsperioden",
      detaljerOgInnhold: [
        {
          _key: "d011968de395",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: ["strong"],
              text: "Kurset inneholder",
              _key: "02171cf5f4bc",
            },
          ],
          _type: "block",
          style: "normal",
        },
        {
          style: "normal",
          _key: "1b685e983d5c",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "\nDeltakere skal veiledes for å finne strategier for å fungere i arbeidslivet til tross for sykdom og skade samt tilbys løsningsrettede aktiviteter som legger til rette for arbeidsaktivitet.\n\nLeverandør skal tilby individuelt tilpasset løsningsrettet bistand for at deltaker skal kunne bevare arbeid eller styrke sine muligheter for å komme i arbeid, herunder: ",
              _key: "fe9c1fdef4bb",
            },
          ],
          _type: "block",
        },
        {
          _key: "67185d944042",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Motivasjons- og mestringsaktiviteter",
              _key: "24dd6cb0dc23",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
        },
        {
          _key: "8f88c5050640",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Individuelt treningsopplegg med veiledning",
              _key: "a0e59237e422",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
        },
        {
          level: 1,
          _type: "block",
          style: "normal",
          _key: "5016a43c1c0a",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              marks: [],
              text: "Arbeidsutprøving i trygge omgivelser",
              _key: "9dbefd241c2e",
              _type: "span",
            },
          ],
        },
        {
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Kontakt med arbeidslivet",
              _key: "62669bcace7b",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
          _key: "024dd387a934",
          listItem: "bullet",
        },
        {
          _key: "07f16758b032",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Livsstilsveiledning",
              _key: "0980191c9c63",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
        },
      ],
    },
  },
  jobbklubb: {
    sanityId: "31e72dd8-ad05-4e81-a7f9-fd4c8f295864",
    delingMedBruker:
      "Jobbklubb er et kortvarig tiltak for deg som søker jobb. Når du deltar på jobbklubb, får du støtte og hjelp til å orientere deg på arbeidsmarkedet og være en aktiv jobbsøker.\n\nDu kan lese mer om kurset på www.nav.no/jobbklubb",
    regelverkLenker: [
      {
        _id: "123",
        regelverkLenkeNavn: "Regelverk",
        regelverkUrl: "https://www.google.no",
      },
      {
        _id: "1234",
        regelverkLenkeNavn: "Rundskriv",
        regelverkUrl: "https://www.google.no",
      },
    ],
    faneinnhold: {
      forHvem: [
        {
          children: [
            {
              text: "Jobbklubb passer for personer som er: ",
              _key: "da0cc37a68d1",
              _type: "span",
              marks: ["strong"],
            },
          ],
          _type: "block",
          style: "normal",
          _key: "afadbb4cd92a",
          markDefs: [],
        },
        {
          style: "normal",
          _key: "d273f9464a05",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _key: "712341b72d5f",
              _type: "span",
              marks: [],
              text: "Helt eller delvis arbeidsledige eller permitert",
            },
          ],
          level: 1,
          _type: "block",
        },
        {
          children: [
            {
              _key: "82bc1dd112f0",
              _type: "span",
              marks: [],
              text: "Registrert som arbeidssøker",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
          _key: "adaff38853d4",
          listItem: "bullet",
          markDefs: [],
        },
        {
          style: "normal",
          _key: "7353f07c815a",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _key: "89881e8a96a8",
              _type: "span",
              marks: [],
              text: "Vil bli en bedre og mer aktiv jobbsøker",
            },
          ],
          level: 1,
          _type: "block",
        },
        {
          _type: "block",
          style: "normal",
          _key: "8ccd3838629c",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              text: "Har behov for økte ferdigheter og kompetanse i jobbsøkerprosessen",
              _key: "3e817260560e",
              _type: "span",
              marks: [],
            },
          ],
          level: 1,
        },
        {
          _key: "11fb2dd390d9",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "",
              _key: "ccfb08e3ef9f",
            },
          ],
          _type: "block",
          style: "normal",
        },
      ],
      detaljerOgInnhold: [
        {
          _key: "431111d08f52",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: ["strong"],
              text: "Kurset inneholder:",
              _key: "25d3174960bc",
            },
          ],
          _type: "block",
          style: "normal",
        },
        {
          children: [
            {
              _type: "span",
              marks: [],
              text: "Veiledning til å se egen kompetanse",
              _key: "d3a7c4f15d7e",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
          _key: "f76dce45e145",
          listItem: "bullet",
          markDefs: [],
        },
        {
          _key: "14c268ff77af",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Lære å markedsføre kompetansen overfor arbeidsgivere",
              _key: "6bcc0fad164b",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
        },
        {
          markDefs: [],
          children: [
            {
              _key: "9fcf7dce8b5b",
              _type: "span",
              marks: [],
              text: "Opplæring i å utforme CV og søknader til aktuelle jobber",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
          _key: "a82bd2284bda",
          listItem: "bullet",
        },
        {
          style: "normal",
          _key: "65103c9e9aa5",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              text: "Få informasjon om arbeidsmarkedet lokalt og nasjonalt",
              _key: "9f80b8b2edff",
              _type: "span",
              marks: [],
            },
          ],
          level: 1,
          _type: "block",
        },
        {
          _type: "block",
          style: "normal",
          _key: "23f759676c1d",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Intervjutrening og opplæring i hvordan bruke eget nettverket for å skaffe jobb.",
              _key: "d150dbfdf29b",
            },
          ],
          level: 1,
        },
        {
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "",
              _key: "b17e37f8b6be",
            },
          ],
          _type: "block",
          style: "normal",
          _key: "5dd4d5bf2e8b",
        },
      ],
      pameldingOgVarighet: [
        {
          markDefs: [],
          children: [
            {
              _key: "01e33a0b1bbf",
              _type: "span",
              marks: ["strong"],
              text: "Digitalt oppfølgingstiltak bør også vurderes før bruker søkes inn på ordinær jobbklubb. Det gjelder ikke for Øst-Viken, Vest-Viken eller Innlandet.",
            },
          ],
          _type: "block",
          style: "normal",
          _key: "b2c4e56a4875",
        },
        {
          style: "normal",
          _key: "4d676de543cb",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "",
              _key: "91a7568908bd",
            },
          ],
          _type: "block",
        },
        {
          style: "normal",
          _key: "98ce2dbc5da8",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Bruker søkes inn via arena. ",
              _key: "050fb77744b3",
            },
          ],
          _type: "block",
        },
      ],
    },
    innsatsgrupper: [
      Innsatsgruppe.STANDARD_INNSATS,
      Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
      Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
      Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
      Innsatsgruppe.VARIG_TILPASSET_INNSATS,
    ],
    beskrivelse:
      "Jobbklubb er et kortvarig tiltak for de som søker jobb. Ved deltagelse på jobbklubb får brukeren støtte og hjelp til å orientere seg på arbeidsmarkedet og være en aktiv jobbsøker.",
    navn: "Jobbklubb",
  },
  digital_jobbklubb: {
    innsatsgrupper: [
      Innsatsgruppe.STANDARD_INNSATS,
      Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
      Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
      Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
      Innsatsgruppe.VARIG_TILPASSET_INNSATS,
    ],
    delingMedBruker:
      "Vi har nå et kurs som jeg tenker kan passe deg godt. Det heter <tiltaksnavn>, og er et heldigitalt jobbsøkings- og karriereveiledningskurs.\n\nMålet er at du skal komme i jobb. Du \n- trener på jobbintervju\n- lærer hvordan skrive CV og jobbsøknad\n- får individuell karriereveileding\n- lærer om arbeidsmarkedet og hvor du finner ledige jobber\n- deltar på gruppearbeid og deler erfaringer med andre arbeidssøkere\n- får hjelp til å søke jobber\n\nDu kan lese mer om kurset  på www.nav.no/digital-jobbklubb ",
    beskrivelse:
      "Digital oppfølgingstiltak er et kortvarig nettbasert kurs som skal gi  karriereveiledning, jobbsøkningsbistand og personlig oppfølging med egen rådgiver. Målet er at tiltaksdeltaker gjennom veiledning og et individuelt tilpasset opplegg  skal bli selvforsørget. ",
    navn: 'Digitalt oppfølgingstiltak for arbeidsledige ("digital jobbklubb")',
    faneinnhold: {
      forHvemInfoboks:
        "Avtalen med AS3 er sagt opp, så brukere fra Oslo. Øst-Viken, Vest-Viken og Innlandet har ikke lenger tilgang til Digital jobbklubb",
      pameldingOgVarighet: [
        {
          style: "normal",
          _key: "ca0f44e225eb",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "1. ",
              _key: "bd08bd6b6134",
            },
            {
              _type: "span",
              marks: ["strong"],
              text: "Ta kontakt med bruker du vurderer å søke inn til tiltaket",
              _key: "aa97a9f30d07",
            },
          ],
          _type: "block",
        },
        {
          _key: "4f02cf68e842",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Forsikre deg om at bruker fyller kriteriene for tiltaket",
              _key: "d1c193f08b9a",
            },
          ],
          _type: "block",
          style: "normal",
        },
        {
          style: "normal",
          _key: "ff558f441847",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "",
              _key: "cac755ddff3d",
            },
          ],
          _type: "block",
        },
        {
          children: [
            {
              marks: [],
              text: "2. ",
              _key: "00528613dfcb",
              _type: "span",
            },
            {
              _type: "span",
              marks: ["strong"],
              text: "Søk inn på ditt fylkes tiltaksgjennomføring i Arena",
              _key: "216e96e14816",
            },
          ],
          _type: "block",
          style: "normal",
          _key: "9f1924a39288",
          markDefs: [],
        },
        {
          markDefs: [],
          children: [
            {
              marks: [],
              text: "",
              _key: "e252b31bde5e",
              _type: "span",
            },
          ],
          _type: "block",
          style: "normal",
          _key: "61a8f1c8a608",
        },
        {
          markDefs: [],
          children: [
            {
              _key: "3409653ada1d",
              _type: "span",
              marks: [],
              text: "3. ",
            },
            {
              _type: "span",
              marks: ["strong"],
              text: "Meld på deltaker til leverandør samme dag",
              _key: "81978517a0eb",
            },
          ],
          _type: "block",
          style: "normal",
          _key: "c384cac0dbc6",
        },
        {
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Dette gjøres via en <lenke til digital påmeldingsløsning>",
              _key: "02fbf664cc9a",
            },
          ],
          _type: "block",
          style: "normal",
          _key: "d1a97e110bce",
        },
        {
          markDefs: [],
          children: [
            {
              marks: [],
              text: "",
              _key: "e528c7a8061a",
              _type: "span",
            },
          ],
          _type: "block",
          style: "normal",
          _key: "02265d3ed04a",
        },
        {
          children: [
            {
              _type: "span",
              marks: [],
              text: "4. ",
              _key: "5499f7a239b1",
            },
            {
              _type: "span",
              marks: ["strong"],
              text: "Tiltaksarrangør tar kontakt med brukeren",
              _key: "0a1d41c64a1c",
            },
          ],
          _type: "block",
          style: "normal",
          _key: "5ab030a5fe50",
          markDefs: [],
        },
        {
          _key: "1613ad9d467a",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Tiltaksarrangør tar kontakt med brukeren på e-post og eventuelt SMS og avtaler oppstart.",
              _key: "91992bbecd2d",
            },
          ],
          _type: "block",
          style: "normal",
        },
        {
          children: [
            {
              _key: "689d7db526e1",
              _type: "span",
              marks: [],
              text: "",
            },
          ],
          _type: "block",
          style: "normal",
          _key: "cf8cc491b3c4",
          markDefs: [],
        },
        {
          _key: "56d752d6ec6d",
          markDefs: [],
          children: [
            {
              text: "Det er ikke nødvendig å skrive en bestilling eller sende med utfyllende dokumentasjon til leverandør. Melding om plass til bruker (vedtaksbrev) er digital.",
              _key: "9d24d9085bdb",
              _type: "span",
              marks: [],
            },
          ],
          _type: "block",
          style: "normal",
        },
      ],
      forHvem: [
        {
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: ["strong"],
              text: "Hvem kan delta: ",
              _key: "1a7fec660eb0",
            },
          ],
          _type: "block",
          style: "normal",
          _key: "ed4e92ba060b",
        },
        {
          _key: "dfbd509e2749",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Helt eller delvis arbeidsledige eller permitert",
              _key: "283b30204f67",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
        },
        {
          _key: "c1cf471258e3",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Brukere som har grunnleggende digital kompetanse",
              _key: "169f065ed05c",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
        },
        {
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Brukere med grunnleggende norskkunskaper",
              _key: "b5539b4a1025",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
          _key: "2467af3ec29f",
          listItem: "bullet",
        },
        {
          markDefs: [],
          children: [
            {
              marks: [],
              text: "Bruker må ha tilgang til PC og internett",
              _key: "8edeba1511d2",
              _type: "span",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
          _key: "fb3b14d21934",
          listItem: "bullet",
        },
        {
          _type: "block",
          style: "normal",
          _key: "760688aeda91",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              marks: [],
              text: "Bruker kan ikke ha omfattende helseproblematikk",
              _key: "ae920be7d163",
              _type: "span",
            },
          ],
          level: 1,
        },
      ],
      detaljerOgInnhold: [
        {
          _key: "d451fced538c",
          markDefs: [],
          children: [
            {
              _key: "9f10f3601f46",
              _type: "span",
              marks: [],
              text: "Tiltaket skal bidra til at deltakerne blir aktive arbeidssøkere gjennom mobilisering av egen kompetanse, interesser og erfaringer. ",
            },
          ],
          _type: "block",
          style: "normal",
        },
        {
          markDefs: [],
          children: [
            {
              marks: [],
              text: "",
              _key: "13dafaee11a3",
              _type: "span",
            },
          ],
          _type: "block",
          style: "normal",
          _key: "5f02bfbefad5",
        },
        {
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Gjennom individuelt tilpasset opplæring og oppfølging skal deltakerne utvikle kunnskaper og ferdigheter som bidrar til at de kommer i arbeid og styrker mulighetene for å kunne stå i jobb over tid.",
              _key: "c5b3405215d3",
            },
          ],
          _type: "block",
          style: "normal",
          _key: "98c95db1bd27",
        },
        {
          markDefs: [],
          children: [
            {
              text: "",
              _key: "974c7137d4af",
              _type: "span",
              marks: [],
            },
          ],
          _type: "block",
          style: "normal",
          _key: "a0310650e970",
        },
        {
          markDefs: [],
          children: [
            {
              marks: ["strong"],
              text: "Kurset inneholder:",
              _key: "badd338bfe4e",
              _type: "span",
            },
          ],
          _type: "block",
          style: "normal",
          _key: "6de79ffa97be",
        },
        {
          level: 1,
          _type: "block",
          style: "normal",
          _key: "8f23e22f7b90",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _key: "e5b2595d0a65",
              _type: "span",
              marks: [],
              text: "Kartlegging av kompetanse, ferdigheter og interesser",
            },
          ],
        },
        {
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Karriereveiledning",
              _key: "fc34e0e36a2f",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
          _key: "47f0e2aaaf19",
        },
        {
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Bistand til jobbsøking, inkludert veiledning om utfylling av CV og jobbprofil på arbeidsplassen.no",
              _key: "d6781093c918",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
          _key: "65fcbec1c91d",
          listItem: "bullet",
        },
        {
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              marks: [],
              text: "Informasjon om muligheter i arbeidsmarkedet",
              _key: "e371cbf080ae",
              _type: "span",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
          _key: "76619dbf90a7",
        },
        {
          _type: "block",
          style: "normal",
          _key: "24619be52eb7",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Råd om kontakt med arbeidsgivere og oppsøkende virksomhet\n",
              _key: "a35e3a335026",
            },
          ],
          level: 1,
        },
        {
          _type: "block",
          style: "normal",
          _key: "324a6d70ecbe",
          markDefs: [],
          children: [
            {
              text: "Det vil være både individuell aktivitet og gruppeaktivitet med refleksjon og erfaringsutveksling, samt nettverksmøter.\n\nTiltaket passer godt både svært tidlig i jobbsøkingsprosessen og for de som har vært arbeidsledige en god stund. Brukere med omfattende helseproblematikk eller andre betydelige hindre for arbeidsdeltakelse er ikke aktuelle for tiltaket.",
              _key: "e74dc5db6701",
              _type: "span",
              marks: [],
            },
          ],
        },
      ],
    },
    sanityId: "3526de0d-ad4c-4b81-b072-a13b3a4b4ed3",
    regelverkLenker: [
      {
        _id: "123",
        regelverkLenkeNavn: "Regelverk",
        regelverkUrl: "https://www.google.no",
      },
      {
        _id: "1234",
        regelverkLenkeNavn: "Rundskriv",
        regelverkUrl: "https://www.google.no",
      },
    ],
  },
  opplaring_fagskole: {
    regelverkLenker: [
      {
        _id: "123",
        regelverkLenkeNavn: "Regelverk",
        regelverkUrl: "https://www.google.no",
      },
      {
        _id: "1234",
        regelverkLenkeNavn: "Rundskriv",
        regelverkUrl: "https://www.google.no",
      },
    ],
    beskrivelse:
      "Opplæringstiltak i form av fagskole (høyere yrkesfaglig utdanning - jf. tiltaksforskriften § 7-2 b) skal bidra til at arbeidssøkere kvalifiseres til ledige jobber. \n\nFagskole er yrkesfaglig utdanning på nivået over videregående opplæring.",
    navn: "Opplæring - Fagskole (høyere yrkesfaglig utdanning)",
    faneinnhold: {
      forHvem: [
        {
          children: [
            {
              _type: "span",
              marks: [],
              text: "Brukere med behov for kvalifisering for å komme inn på arbeidsmarkedet",
              _key: "a58b01410e32",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
          _key: "02cdbd145991",
          listItem: "bullet",
          markDefs: [],
        },
        {
          _key: "abd81dab97bb",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Brukere få opplæringstiltak i form av fagskole fra det året de fyller 19 år",
              _key: "3f90fb0dc290",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
        },
        {
          children: [
            {
              text: "Åpent for brukere med situasjonsbestemt, spesielt tilpasset og varig tilpasset innsats",
              _key: "053b3e2cd225",
              _type: "span",
              marks: [],
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
          _key: "cdab2bcedd59",
          listItem: "bullet",
          markDefs: [],
        },
      ],
      pameldingOgVarighet: [
        {
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: 'Fagskole skal registreres som "Enkeltplass Fag- og yrkesopplæring VGS og høyere yrkesfaglig utdanning" i Arena. ',
              _key: "e66bf9fd76d7",
            },
          ],
          _type: "block",
          style: "normal",
          _key: "505eb8dc6369",
        },
        {
          children: [
            {
              marks: [],
              text: "Tiltaket kan gis i inntil tre år, med mulighet for forlengelse i ett år dersom tiltaksdeltakeren av særlige grunner ikke kan gjennomføre utdanningen i løpet av tre år.",
              _key: "31574ea485ed",
              _type: "span",
            },
          ],
          _type: "block",
          style: "normal",
          _key: "2942c687ec37",
          markDefs: [],
        },
      ],
    },
    innsatsgrupper: [
      Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
      Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
      Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
      Innsatsgruppe.VARIG_TILPASSET_INNSATS,
    ],
    sanityId: "50878ad5-90d0-496d-a0d0-a53091800760",
  },
  oppfolging: {
    faneinnhold: {
      detaljerOgInnhold: [
        {
          children: [
            {
              _key: "4055ffd45fcc",
              _type: "span",
              marks: ["strong"],
              text: "Kurset inneholder:",
            },
          ],
          _type: "block",
          style: "normal",
          _key: "3cfbd1521f0d",
          markDefs: [],
        },
        {
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              marks: [],
              text: "Karriereveiledning og bistand til å se egne muligheter på arbeidsmarkedet",
              _key: "8775481b34fc",
              _type: "span",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
          _key: "292803c750b6",
        },
        {
          _key: "bac19486b808",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Bistand til målrettet jobbsøking og jobbutvikling",
              _key: "7b1311b7e8f5",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
        },
        {
          _key: "ac07e50066d3",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Veiledning og råd knyttet til arbeidsdeltakelse både til tiltaksdeltaker og arbeidsgivere",
              _key: "8b7599f273ca",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
        },
        {
          children: [
            {
              marks: [],
              text: "Opplæring i arbeidsrelaterte og sosiale ferdigheter som er nødvendige for å komme i og beholde arbeid",
              _key: "2cf728ee3eb8",
              _type: "span",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
          _key: "a3e47f000ee8",
          listItem: "bullet",
          markDefs: [],
        },
        {
          _type: "block",
          style: "normal",
          _key: "beaf9e2141be",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Oppfølging på arbeidsplassen for å legge til rette for overgang til arbeid",
              _key: "ca5bc9a7b629",
            },
          ],
          level: 1,
        },
        {
          style: "normal",
          _key: "53d0e940a35c",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Oppfølging og støtte til både tiltaksdeltaker og arbeidsgiver for å sikre jobbfastholdelse og videre karriereutvikling",
              _key: "7f3a31e9d70f",
            },
          ],
          level: 1,
          _type: "block",
        },
        {
          _key: "954dafe2e16f",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Bistand til tilpasning og tilrettelegging av arbeid og arbeidssituasjonen",
              _key: "c4f9160b6d7e",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
        },
      ],
      pameldingOgVarighet: [
        {
          _type: "block",
          style: "normal",
          _key: "1fd74ba13c7e",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Oppfølgingstiltaket kan vare i inntil seks måneder. Tiltaket kan forlenges med ytterligere seks måneder. For personer med nedsatt arbeidsevne kan tiltaket forlenges slik at samlet varighet kan være inntil tre år. Om tiltaket brukes ved overgang fra skole eller soning i institusjon kan varigheten forlenges med ytterligere seks måneder utover den maksimale varigheten på tre år.",
              _key: "fca69f05e3f2",
            },
          ],
        },
        {
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "\nVarigheten av oppfølgingen skal tilpasses deltakerens individuelle behov. Timeforbruket vil derfor variere fra deltaker til deltaker. NAV antar at et gjennomsnittsbehov vil være om lag 10 timer oppfølging per deltaker per måned i hele tiltaksperioden. Det understrekes imidlertid at hver enkelt deltaker skal få det antallet oppfølgingstimer som er nødvendig for at deltaker skal nå målet om å få og/eller beholde arbeid. ",
              _key: "d919e0801ee3",
            },
          ],
          _type: "block",
          style: "normal",
          _key: "bb7c838ed366",
        },
        {
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "",
              _key: "8041c2fd8cc7",
            },
          ],
          _type: "block",
          style: "normal",
          _key: "6a539d22b7cc",
        },
        {
          _key: "eda589da650b",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Varig tilknytning til arbeidslivet er en viktig del av målsettingen for tiltaket. Derfor skal tiltaket som hovedregel fortsette etter ansettelse for å sikre at tiltaksdeltaker forblir i jobb, med mindre deltaker ikke selv ønsker dette.",
              _key: "111d21042545",
            },
          ],
          _type: "block",
          style: "normal",
        },
        {
          style: "normal",
          _key: "c691dc9cfc7f",
          markDefs: [],
          children: [
            {
              text: "",
              _key: "6d2d609f69e3",
              _type: "span",
              marks: [],
            },
          ],
          _type: "block",
        },
        {
          style: "normal",
          _key: "77f1feafa8a0",
          markDefs: [],
          children: [
            {
              text: "Se også Digitalt oppfølgingstiltak for arbeidsledige (Digital jobbklubb)",
              _key: "491b563474de",
              _type: "span",
              marks: [],
            },
          ],
          _type: "block",
        },
        {
          style: "normal",
          _key: "00178238cca4",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "",
              _key: "2ee2189b4b73",
            },
          ],
          _type: "block",
        },
      ],
      forHvem: [
        {
          _type: "block",
          style: "normal",
          _key: "ef7b2d7b5d75",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: ["strong"],
              text: "Kriterier for deltagelse:",
              _key: "5146097f8773",
            },
          ],
        },
        {
          children: [
            {
              text: "Brukere som har behov for individuell oppfølging",
              _key: "8e73ef8fa88f",
              _type: "span",
              marks: [],
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
          _key: "41beaab8a63f",
          listItem: "bullet",
          markDefs: [],
        },
        {
          level: 1,
          _type: "block",
          style: "normal",
          _key: "f729dcd21ed5",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Brukere som har et ønske om å skaffe og/eller beholde jobb",
              _key: "4995986a2591",
            },
          ],
        },
        {
          markDefs: [],
          children: [
            {
              text: "For sykmeldte arbeidstakere er det ikke nødvendig med behovs-/arbeidsevnevurdering og 14a-vedtak.",
              _key: "3d34832ce163",
              _type: "span",
              marks: [],
            },
          ],
          _type: "block",
          style: "normal",
          _key: "067716417e48",
        },
      ],
    },
    innsatsgrupper: [
      Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
      Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
      Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
      Innsatsgruppe.VARIG_TILPASSET_INNSATS,
    ],
    delingMedBruker:
      "Vi har nå et arbeidsmarkedstiltak som jeg tenker kan passe deg godt. Det heter <tiltaksnavn>  og vil tilpasses dine behov for støtte for å skaffe eller beholde en jobb.\n\nDu kan blant annet få veiledning, råd og bistand til\n- jobbsøking og karriereveiledning\n- tilpasning og tilrettelegging av arbeidet og arbeidssituasjonen\n- praktiske oppgaver knyttet til arbeid\n- opplæring i sosiale og arbeidsrelaterte ferdigheter i arbeidslivet\n\nDu kan lese mer om kurset på www.nav.no/oppfolging",
    navn: "Oppfølging",
    sanityId: "5ac48c03-1f4c-4d4b-b862-050caca92080",
    regelverkLenker: [
      {
        _id: "123",
        regelverkLenkeNavn: "Regelverk",
        regelverkUrl: "https://www.google.no",
      },
      {
        _id: "1234",
        regelverkLenkeNavn: "Rundskriv",
        regelverkUrl: "https://www.google.no",
      },
    ],
    beskrivelse:
      "Oppfølging skal gi bistand med sikte på at tiltaksdeltakere skaffer seg og/eller beholder lønnet arbeid. Målet er at tiltaksdeltaker i størst mulig grad skal bli selvforsørget med en varig tilknytning til arbeidslivet. ",
  },
  varig_lonnstilskudd: {
    delingMedBruker:
      "Vi har et tilbud som jeg tenker kan passe for deg.  \n\nDet heter varig lønnstilskudd og er for deg med en varig og vesentlig nedsatt arbeidsevne. Tiltaket skal øke mulighetene for at du kan få en vanlig jobb. Du blir ansatt med vanlig lønn i en hel- eller deltidsstilling, mens arbeidsgiveren får et tilskudd til lønnen din. \n\nDu kan lese mer om tilbudet på https://www.nav.no/varig-lonnstilskudd",
    beskrivelse:
      "Varig lønnstilskudd skal bidra til å skaffe eller beholde arbeid, samt motvirke overgang til uføre for personer med varig og vesentlig nedsatt arbeidsevne",
    regelverkLenker: [
      {
        _id: "123",
        regelverkLenkeNavn: "Regelverk",
        regelverkUrl: "https://www.google.no",
      },
      {
        _id: "1234",
        regelverkLenkeNavn: "Rundskriv",
        regelverkUrl: "https://www.google.no",
      },
    ],
    navn: "Varig lønnstilskudd",
    faneinnhold: {
      forHvemInfoboks:
        "Arbeidsgiver kan ikke motta lønnstilskudd for lærlinger. Dette ble stanset i juli 2017 og gjelder til annet blir bestemt",
      pameldingOgVarighet: [
        {
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Tilskudd kan gis uten tidsavgrensning, og kan utbetales så lenge det er nødvendig og hensiktsmessig. Tilskuddet skal vurderes minst hvert halvår i samarbeid med virksomheten. Ved bedring i arbeidsevnen eller når andre tiltak vurderes som mer hensiktsmessige, skal tilskuddet reduseres eller falle bort",
              _key: "f00f9047158f",
            },
          ],
          _type: "block",
          style: "normal",
          _key: "123a03e3723c",
        },
        {
          _type: "block",
          style: "normal",
          _key: "1cd3946312a1",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "",
              _key: "4a1acc29ad43",
            },
          ],
        },
        {
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Må ikke stanses ved 67 år - arbeidsmiljøloven",
              _key: "7347f948f46f",
            },
          ],
          _type: "block",
          style: "normal",
          _key: "b534d0f6766c",
        },
        {
          _key: "31f38d084e81",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "",
              _key: "ed68e9743df7",
            },
          ],
          _type: "block",
          style: "normal",
        },
        {
          markDefs: [],
          children: [
            {
              marks: [],
              text: "Påmelding til Varig lønnstilskudd tilgjengelig som digital avtale. Det vil si at deltaker, arbeidsgiver og NAV fyller ut, ser over og godkjenner avtalen i samme løsning i sanntid.",
              _key: "99d37d57c8ac",
              _type: "span",
            },
          ],
          _type: "block",
          style: "normal",
          _key: "dd49aa52d549",
        },
        {
          markDefs: [
            {
              _type: "link",
              href: "https://navno.sharepoint.com/sites/intranett-produktomrader-og-utvikling/SitePages/Ny-digital-avtale-om-l%C3%B8nnstilskudd.aspx",
              _key: "29c388192145",
            },
          ],
          children: [
            {
              _type: "span",
              marks: ["29c388192145"],
              text: "Les mer om hvordan digital avtale fungerer",
              _key: "5a5e4923d5dc",
            },
          ],
          _type: "block",
          style: "normal",
          _key: "4d3487d3587c",
        },
      ],
      forHvem: [
        {
          _type: "block",
          style: "normal",
          _key: "55ac02e0b194",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "For personer med vesentlig- og varig nedsatt arbeidsevne.\nTilskuddet gis arbeidsgivere som ansetter på ordinære lønns- og arbeidsvilkår. Tilskuddet kan også gis til arbeidsgivere som beholder arbeidstakere med nedsatt arbeidsevne, som står i fare for å falle ut etter tolv måneder med full eller gradert sykmelding.",
              _key: "85204e431653",
            },
          ],
        },
      ],
      detaljerOgInnhold: [
        {
          _type: "block",
          style: "normal",
          _key: "a9f1d8d5cd71",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Ordningen innebærer at arbeidsgiver ansetter i hel- eller deltidsstilling, men får et tilskudd til lønnen. Den enkelte utfører ordinære arbeidsoppgaver i virksomheten, mens arbeidsgiver kompenseres for arbeidstakerens lavere produktivitet.\nTilskuddet kan maksimalt utgjøre 75 prosent av lønn og sosiale utgifter det første året, og 67 prosent de påfølgende årene. Refusjonen kan utgjøre inntil maksimalt fem ganger grunnbeløpet (5G) i folketrygden per år.",
              _key: "1fd645abdcee",
            },
          ],
        },
        {
          style: "normal",
          _key: "2f51fe933716",
          markDefs: [],
          children: [
            {
              _key: "8cabda3bf67a",
              _type: "span",
              marks: [],
              text: "",
            },
          ],
          _type: "block",
        },
        {
          style: "normal",
          _key: "ea8bb8a21fdb",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Oppfølgingen og tilrettelegging skal være individuell og planlegges i nært samarbeid med den enkelte deltaker og arbeidsgiver.\n\nDet skal være en fast kontaktperson i NAV og på arbeidsplassen hvor avtalepartene kan henvende seg",
              _key: "27ee2ce6b915",
            },
          ],
          _type: "block",
        },
      ],
    },
    innsatsgrupper: [
      Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
      Innsatsgruppe.VARIG_TILPASSET_INNSATS,
    ],
    sanityId: "6de22004-9fb8-4c84-9b75-dc8132a78cd2",
  },
  grufagyrke: {
    faneinnhold: {
      forHvem: [
        {
          level: 1,
          _type: "block",
          style: "normal",
          _key: "eafe35d07275",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _key: "df9bba407c5f",
              _type: "span",
              marks: [],
              text: "Brukere med behov for kvalifisering for å komme inn på arbeidsmarkedet",
            },
          ],
        },
        {
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _key: "fa485a2c274e",
              _type: "span",
              marks: [],
              text: "Brukere kan starte i fag- og yrkesopplæring det året de fyller 19 år",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
          _key: "07e1a9040488",
        },
        {
          markDefs: [],
          children: [
            {
              _key: "466f16efa29b",
              _type: "span",
              marks: [],
              text: "Åpent for brukere med situasjonsbestemt, spesielt tilpasset og varig tilpasset innsats",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
          _key: "ea0e350ac4ad",
          listItem: "bullet",
        },
      ],
    },
    innsatsgrupper: [
      Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
      Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
      Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
      Innsatsgruppe.VARIG_TILPASSET_INNSATS,
    ],
    delingMedBruker:
      "Vi har nå et opplæringstiltak som jeg tenker kan passe deg godt. Det heter <tiltaksnavn>, og hensikten med tiltaket er å kvalifisere deg for ledige jobber.\n\nDu kan lese mer om tiltaket på www.nav.no/opplaring",
    navn: "Opplæring - Gruppe Fag- og yrkesopplæring",
    sanityId: "7f353dcd-37c2-42f8-bab6-ac2a60669839",
    beskrivelse:
      "Opplæringstiltak i form av Gruppe Fag- og yrkesopplæring (jf. tiltaksforskriften § 7-2 b) skal bidra til at arbeidssøkere kvalifiseres til ledige jobber. ",
  },
  VTAO: {
    faneinnhold: {
      forHvem: [
        {
          style: "normal",
          _key: "b4b668d09d18",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Tiltaket er rettet mot personer som mottar eller i nær fremtid ventes å få innvilget uføretrygd, og som har behov for spesiell tilrettelegging og tett oppfølging.",
              _key: "d5fdb079dfec",
            },
          ],
          _type: "block",
        },
      ],
      detaljerOgInnhold: [
        {
          style: "normal",
          _key: "24a6d0a8e938",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Arbeidsgiver må kunne tilby brukerne et individuelt tilpasset opplegg etter den enkeltes forutsetninger og med nødvendig oppfølging.",
              _key: "223914bace1f",
            },
          ],
          _type: "block",
        },
      ],
      forHvemInfoboks:
        "Tiltaket er rettet mot personer som mottar eller i nær fremtid ventes å få innvilget uføretrygd, og som har behov for spesiell tilrettelegging og tett oppfølging. ",
    },
    innsatsgrupper: [
      Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
      Innsatsgruppe.VARIG_TILPASSET_INNSATS,
    ],
    regelverkLenker: [
      {
        _id: "123",
        regelverkLenkeNavn: "Regelverk",
        regelverkUrl: "https://www.google.no",
      },
      {
        _id: "1234",
        regelverkLenkeNavn: "Rundskriv",
        regelverkUrl: "https://www.google.no",
      },
    ],
    navn: "VTA-O - Varig tilrettelagt arbeid i ordinær virksomhet",

    sanityId: "8d8abebd-3617-494a-a687-d44810e0a7ee",
    beskrivelse:
      "VTA-O er et tiltak som skal gi brukeren arbeid med oppgaver tilpasset den enkeltes arbeidsevne.",
  },
  midlertidig_lonnstilskudd: {
    faneinnhold: {
      forHvemInfoboks:
        "Arbeidsgiver kan ikke motta lønnstilskudd for lærlinger. Dette ble stanset i juli 2017 og gjelder til annet blir bestemt.",
      pameldingOgVarighet: [
        {
          _key: "361d21ec6a11",
          markDefs: [],
          children: [
            {
              text: "Påmelding til midlertidig lønnstilskudd er tilgjengelig som digital avtale. Det vil si at deltaker, arbeidsgiver og NAV fyller ut, ser over og godkjenner avtalen i samme løsning i sanntid.",
              _key: "61b639de745d",
              _type: "span",
              marks: [],
            },
          ],
          _type: "block",
          style: "normal",
        },
        {
          style: "normal",
          _key: "f60b01ddd2c2",
          markDefs: [
            {
              _key: "dc0907f622c7",
              _type: "link",
              href: "https://navno.sharepoint.com/sites/intranett-produktomrader-og-utvikling/SitePages/Ny-digital-avtale-om-l%C3%B8nnstilskudd.aspx",
            },
          ],
          children: [
            {
              _type: "span",
              marks: ["dc0907f622c7"],
              text: "Les mer om hvordan digital avtale fungerer",
              _key: "1d64713c6c3c",
            },
          ],
          _type: "block",
        },
        {
          markDefs: [],
          children: [
            {
              text: "",
              _key: "1a7d7393895a",
              _type: "span",
              marks: [],
            },
          ],
          _type: "block",
          style: "normal",
          _key: "5fa6fe8d8ca3",
        },
        {
          markDefs: [],
          children: [
            {
              marks: [],
              text: "Varigheten på midlertidig lønnstilskudd baseres på individuelle behov. For personer med Situasjonsbestemt innsats kan det gis i inntil ett år. For de med nedsatt arbeidsevne kan det gis i inntil to år.",
              _key: "b662d5ced40e",
              _type: "span",
            },
          ],
          _type: "block",
          style: "normal",
          _key: "62b3f9ee7c6a",
        },
      ],
      forHvem: [
        {
          style: "normal",
          _key: "2bb2f7d4f589",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "For personer med Situasjonsbestemt til Spesielt tilpasset innsats .\nKan gis til arbeidsgivere som ansetter personer som har problemer med å komme inn på arbeidsmarkedet på ordinære lønns- og arbeidsvilkår.\nKan gis til arbeidsgivere som beholder arbeidstakere med nedsatt arbeidsevne som står i fare for å falle ut av arbeidslivet etter tolv måneder med full eller gradert sykemelding.",
              _key: "b2eddb8e1f2c",
            },
          ],
          _type: "block",
        },
      ],
      detaljerOgInnhold: [
        {
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Ordningen innebærer at arbeidsgiver ansetter i hel- eller deltidsstilling, og men får et tilskudd til lønnen. Den enkelte utfører ordinære arbeidsoppgaver i virksomheten, mens arbeidsgiver kompenseres for arbeidstakerens lavere produktivitet. Perioden med midlertidig lønnstilskudd utgjør en viss prosentandel av lønnen i inntil ett år, og i inntil to år for en person med nedsatt arbeidsevne.\nLønnstilskuddet dekker 40 prosent av refusjonsgrunnlaget i inntil seks måneder, og deretter 30 prosent av refusjonsgrunnlaget. For personer med nedsatt arbeidsevne dekker lønnstilskuddet 60 prosent av refusjonsgrunnlaget i inntil tolv måneder, og deretter 50 prosent av refusjonsgrunnlaget.\nNoe om maksgrense? Aldersgrense?",
              _key: "a476fa4eccdd",
            },
          ],
          _type: "block",
          style: "normal",
          _key: "95db2ce1f828",
        },
        {
          style: "normal",
          _key: "8428bb58bc80",
          markDefs: [],
          children: [
            {
              _key: "f82810ec969e",
              _type: "span",
              marks: [],
              text: "",
            },
          ],
          _type: "block",
        },
        {
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "For å få til god og tilpasset oppfølging av bruker er det mulig å kombinere lønnstilskudd med andre tiltak.Midlertidig lønnstilskud kan kombineres med tiltaksvariantene:",
              _key: "dbf0d488a7d5",
            },
          ],
          _type: "block",
          style: "normal",
          _key: "fba7c6a6c657",
        },
        {
          _type: "block",
          style: "normal",
          _key: "f9b8653e5af4",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Oppfølging",
              _key: "f02aa576c64c",
            },
          ],
          level: 1,
        },
        {
          _type: "block",
          style: "normal",
          _key: "6537609ef921",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Mentor",
              _key: "3ed7689bc6fa",
            },
          ],
          level: 1,
        },
        {
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Inkluderingstilskudd",
              _key: "8d4a5be4e2e2",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
          _key: "073a261059fd",
        },
        {
          level: 1,
          _type: "block",
          style: "normal",
          _key: "91a17e9b3a78",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Opplæringstiltak",
              _key: "c2c4308a58c8",
            },
          ],
        },
      ],
    },
    innsatsgrupper: [
      Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
      Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
      Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
      Innsatsgruppe.VARIG_TILPASSET_INNSATS,
    ],
    sanityId: "a97fd87c-d7c1-49af-b3fb-cf5e5c10522a",
    delingMedBruker:
      "Hei <Fornavn>,   \n\nVi har et tilbud til personer som har behov for hjelp med å få eller beholde en fast jobb. Det heter <tiltaksnavn>, og er en støtteordning hvor NAV dekker en del av lønnen til en arbeidstaker i en periode.\n\nMålet er å hjelpe flere med å få eller beholde en fast jobb.  \n\nDu blir ansatt med vanlig lønn i en hel- eller deltidsstilling, mens arbeidsgiveren får et tilskudd til lønnen din fra NAV.  \n\nDu kan lese mer om tilskuddet på https://www.nav.no/midlertidig-lonnstilskudd \n",
    beskrivelse:
      "Lønnstilskudd skal gi bistand til tiltaksdeltakere slik at de skaffer seg og/eller beholder, lønnet arbeid. Målet er at tiltaksdeltaker i størst mulig grad skal bli selvforsørget med en varig tilknytning til arbeidslivet.",
    navn: "Midlertidig lønnstilskudd",
  },
  mentor: {
    arenakode: TiltakskodeArena.MENTOR,
    faneinnhold: {
      pameldingOgVarighet: [
        {
          markDefs: [],
          children: [
            {
              text: "Påmelding til mentor er tilgjengelig som digital avtale. Det vil si at deltaker, arbeidsgiver og NAV fyller ut, ser over og godkjenner avtalen i samme løsning i sanntid. Mentoren er ikke en part i avtalen, men har lesetilgang til det meste i avtalen og må signere en taushetserklæring.",
              _key: "285cf24d8e29",
              _type: "span",
              marks: [],
            },
          ],
          _type: "block",
          style: "normal",
          _key: "bfa7ef914b12",
        },
        {
          markDefs: [
            {
              _type: "link",
              href: "https://navno.sharepoint.com/sites/intranett-produktomrader-og-utvikling/SitePages/Digital-avtale-om-tilskudd-til-mentor.aspx",
              _key: "b340ad4ff199",
            },
          ],
          children: [
            {
              text: "Les mer om hvordan digital avtale fungerer",
              _key: "dc61b6b6e3e6",
              _type: "span",
              marks: ["b340ad4ff199"],
            },
          ],
          _type: "block",
          style: "normal",
          _key: "69a1e2b531a4",
        },
      ],
      forHvem: [
        {
          markDefs: [],
          children: [
            {
              _key: "f6673a021ab8",
              _type: "span",
              marks: [],
              text: "Personer som trenger støtte for å kunne gjennomføre tiltak i form av arbeidstrening, opplæring ved ordinær utdanning, sommerjobb, midlertidig lønnstilskudd eller varig lønnstilskudd. ",
            },
          ],
          _type: "block",
          style: "normal",
          _key: "6691c5e92f46",
        },
        {
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Tilskudd til mentor kan i tillegg gis hvis det er nødvendig for å kunne få eller beholde lønnet arbeid i en ordinær virksomhet.",
              _key: "92b81e68d84f",
            },
          ],
          _type: "block",
          style: "normal",
          _key: "5db2a6cfb6c1",
        },
      ],
      detaljerOgInnhold: [
        {
          style: "normal",
          _key: "0619db5c5582",
          markDefs: [],
          children: [
            {
              marks: [],
              text: "En mentor er en frikjøpt kollega eller en medstudent, som kan gi praktisk hjelp, veiledning og opplæring, tilpasset den enkeltes individuelle behov",
              _key: "03934da38a53",
              _type: "span",
            },
          ],
          _type: "block",
        },
      ],
    },
    delingMedBruker:
      "Vi har nå et kurs som jeg tenker kan passe deg godt. Det heter <tiltaksnavn>, og er et" +
      " fysisk kurs hvor du lærer The Ways of a Jedi.\n\nMålet er at du skal bli en Jedi" +
      " Mester. Du trener sammen med læremester Yoda og lærer blant annet hvordan bruke" +
      " lyssverd, og du får" +
      " individuell oppfølging av andre Jedi Mestere og deltar på gruppearbeid og deler" +
      " erfaringer med andre Jedis. Du kan lese mer om kurset  på" +
      " www.nav.no/mentor ",
    beskrivelse:
      "Mentor skal gi nødvendig bistand til å kunne gjennomføre arbeidsmarkedstiltak, eller for å kunne få eller beholde lønnet arbeid i en ordinær bedrift.",
    navn: "Mentor",
    innsatsgrupper: [
      Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
      Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
      Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
      Innsatsgruppe.VARIG_TILPASSET_INNSATS,
    ],
    sanityId: "ad998fc6-310e-45d4-a056-57732fed87b4",
    regelverkLenker: [
      {
        _id: "123",
        regelverkLenkeNavn: "Regelverk",
        regelverkUrl: "https://www.google.no",
      },
      {
        _id: "1234",
        regelverkLenkeNavn: "Rundskriv",
        regelverkUrl: "https://www.google.no",
      },
    ],
  },
  enkeltplass_amo: {
    innsatsgrupper: [
      Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
      Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
      Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
      Innsatsgruppe.VARIG_TILPASSET_INNSATS,
    ],
    sanityId: "bbb8d042-b30e-4e4a-8cd0-210019b19de3",
    regelverkLenker: [
      {
        _id: "123",
        regelverkLenkeNavn: "Regelverk",
        regelverkUrl: "https://www.google.no",
      },
      {
        _id: "1234",
        regelverkLenkeNavn: "Rundskriv",
        regelverkUrl: "https://www.google.no",
      },
    ],
    faneinnhold: {
      forHvem: [
        {
          level: 1,
          _type: "block",
          style: "normal",
          _key: "64018d5cfcc6",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Brukere med behov for kvalifisering for å komme inn på arbeidsmarkedet",
              _key: "726dfb91e3fe",
            },
          ],
        },
        {
          style: "normal",
          _key: "3c6106d63ee1",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              text: "Brukere kan starte i AMO det året de fyller 19 år",
              _key: "9208614ea909",
              _type: "span",
              marks: [],
            },
          ],
          level: 1,
          _type: "block",
        },
        {
          _key: "d0f3e98452ff",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              marks: [],
              text: "Åpent for brukere med situasjonsbestemt, spesielt tilpasset og varig tilpasset innsats",
              _key: "3035be66fc47",
              _type: "span",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
        },
      ],
      detaljerOgInnhold: [
        {
          _type: "block",
          style: "normal",
          _key: "502dd2371c30",
          markDefs: [],
          children: [
            {
              text: "Enkeltplass AMO skal kvalifisere til ledige jobber, og bestå av innhold som gir formell kompetanse. ",
              _key: "cf06db822227",
              _type: "span",
              marks: [],
            },
          ],
        },
        {
          style: "normal",
          _key: "eaa18cc56978",
          markDefs: [],
          children: [
            {
              _key: "1f357a0bd5c5",
              _type: "span",
              marks: [],
              text: "Enkeltfag fra studiespesialiserende videregående opplæring som tas med mål om å oppnå studiekompetanse kan gis som Enkeltplass AMO. ",
            },
          ],
          _type: "block",
        },
        {
          children: [
            {
              text: "Norskopplæring eller opplæring i grunnleggende ferdigheter kan også gis som Enkeltplass AMO. ",
              _key: "cd4857444c21",
              _type: "span",
              marks: [],
            },
          ],
          _type: "block",
          style: "normal",
          _key: "3a0a48a942d3",
          markDefs: [],
        },
      ],
      pameldingOgVarighet: [
        {
          style: "normal",
          _key: "fd7f67b54208",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Tiltaket kan gis i inntil tre år.",
              _key: "74ec60518767",
            },
          ],
          _type: "block",
        },
      ],
    },
    beskrivelse:
      "Opplæringstiltak i form av Enkeltplass AMO (jf. tiltaksforskriften § 7-2 a) består av kortvarige kurs basert på behov i arbeidsmarkedet, og skal bidra til at arbeidssøkere kvalifiseres til ledige jobber.",
    navn: "Opplæring - Enkeltplass AMO",
  },
  AFT: {
    beskrivelse:
      "AFT er et tiltak som skal bidra til å prøve ut tiltaksdeltakerens arbeidsevne og til å styrke mulighetene for å få ordinært arbeid. Det er et tiltak med individuell tilrettelegging som kan tilbys personer med sammensatte bistandsbehov som har fått sin arbeidsevne nedsatt og som har særlig usikre yrkesmessige forutsetninger.",

    regelverkLenker: [
      {
        _id: "123",
        regelverkLenkeNavn: "Regelverk",
        regelverkUrl: "https://www.google.no",
      },
      {
        _id: "1234",
        regelverkLenkeNavn: "Rundskriv",
        regelverkUrl: "https://www.google.no",
      },
    ],
    sanityId: "d03363e0-7d46-411b-aec4-fb9449e30eb8",
    navn: "AFT - Arbeidsforberedende trening",
    faneinnhold: {
      forHvem: [
        {
          _type: "block",
          style: "normal",
          _key: "11838589f53b",
          markDefs: [],
          children: [
            {
              _key: "ae9285699b2d",
              _type: "span",
              marks: [],
              text: "AFT er et individuelt tiltak som kan tilbys personer med sammensatte bistandsbehov som har fått sin arbeidsevne nedsatt og som har særlig usikre yrkesmessige forutsetninger.",
            },
          ],
        },
      ],
      detaljerOgInnhold: [
        {
          children: [
            {
              _type: "span",
              marks: [],
              text: "Det skal være individuelt tilpasset og tilrettelagt brukerens behov, og kan inneholde:",
              _key: "efae61d95684",
            },
          ],
          _type: "block",
          style: "normal",
          _key: "0563b7b58f3e",
          markDefs: [],
        },
        {
          level: 1,
          _type: "block",
          style: "normal",
          _key: "0afce2c1e8fc",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Avklaring og kartlegging av ressurser",
              _key: "2aaf9c415a20",
            },
          ],
        },
        {
          style: "normal",
          _key: "9d9ec76d1ef9",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              marks: [],
              text: "Karriereveiledning",
              _key: "2f578003a710",
              _type: "span",
            },
          ],
          level: 1,
          _type: "block",
        },
        {
          markDefs: [],
          children: [
            {
              _key: "f07a9b815d8d",
              _type: "span",
              marks: [],
              text: "Utprøving av arbeidsevne i et tilrettelagt arbeidsmiljø",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
          _key: "a44028e88775",
          listItem: "bullet",
        },
        {
          _key: "990c7a9b466f",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              marks: [],
              text: "Opplæring med sikte på å oppnå reelle ferdigheter og formell kompetanse",
              _key: "b4171fad86f6",
              _type: "span",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
        },
        {
          level: 1,
          _type: "block",
          style: "normal",
          _key: "c274808be39f",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              text: "Arbeidstrening og oppfølging i ordinært arbeidsliv",
              _key: "56d70aad3d65",
              _type: "span",
              marks: [],
            },
          ],
        },
        {
          _key: "c274808be39f_deduped_6",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _key: "56d70aad3d65",
              _type: "span",
              marks: [],
              text: "Arbeidstrening og oppfølging i ordinært arbeidsliv",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
        },
      ],
      pameldingOgVarighet: [
        {
          _type: "block",
          style: "normal",
          _key: "db4fe84d3fd3",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Varigheten skal tilpasses deltagerens individuelle behov, og kan vare i inntil ett år, med mulighet for forlengelse i ytterligere ett år.",
              _key: "5e130c0d19be",
            },
          ],
        },
        {
          style: "normal",
          _key: "b7241288003d",
          markDefs: [],
          children: [
            {
              marks: [],
              text: "",
              _key: "c3168f988e7c",
              _type: "span",
            },
          ],
          _type: "block",
        },
        {
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Det finnes ikke et eget innsøkingsskjema til AFT. Innsøket gjøres i Arena og det genereres en henvisning som skal oversendes arrangør.",
              _key: "4b0e52391dc5",
            },
          ],
          _type: "block",
          style: "normal",
          _key: "f531b753b7d7",
        },
      ],
    },
    innsatsgrupper: [
      Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
      Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
      Innsatsgruppe.VARIG_TILPASSET_INNSATS,
    ],
    delingMedBruker:
      "Vi har nå et arbeidsmarkedstiltak som jeg tenker kan passe deg godt. Det heter <tiltaksnavn> og er et tilbud for deg som deg som har nedsatt arbeidsevne og trenger hjelp for å komme i jobb.\n\nMålet er å avklare arbeidsevnen din gjennom arbeidstrening i ulike arbeidssituasjoner.\n\n- Etter en periode med forberedende arbeidstrening i et tilrettelagt arbeidsmiljø får du arbeidstrening i en vanlig bedrift.\n- Du får kartlagt kompetansen din og får karriereveiledning.\n- Du kan få tilrettelagt opplæring hvis du ønsker å gå videre med et yrkesfaglig utdanningsløp. Opplæringen skal bedre mulighetene dine til å komme i jobb.\n\nDu kan lese mer om kurset på www.nav.no/arbeidsforberedende-trening",
  },
  arbeidstrening: {
    innsatsgrupper: [
      Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
      Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
      Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
      Innsatsgruppe.VARIG_TILPASSET_INNSATS,
    ],
    sanityId: "e8406a67-fabe-4da6-804c-c77a33aaf67d",
    navn: "Arbeidstrening",
    faneinnhold: {
      pameldingOgVarighet: [
        {
          _key: "4d7b5c951a68",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Påmelding til arbeidstrening er tilgjengelig som digital avtale. Det vil si at deltaker, arbeidsgiver og NAV fyller ut, ser over og godkjenner avtalen i samme løsning i sanntid.",
              _key: "cc623cbb13d5",
            },
          ],
          _type: "block",
          style: "normal",
        },
        {
          _key: "d0b6f22461bc",
          markDefs: [
            {
              _type: "link",
              href: "https://navno.sharepoint.com/sites/intranett-produktomrader-og-utvikling/SitePages/Digital-avtale-om-arbeidstrening(1).aspx",
              _key: "22b362f01ba1",
            },
          ],
          children: [
            {
              _type: "span",
              marks: ["22b362f01ba1"],
              text: "Les mer om hvordan digital avtale fungerer",
              _key: "7fc55a146600",
            },
          ],
          _type: "block",
          style: "normal",
        },
      ],
      forHvem: [
        {
          _type: "block",
          style: "normal",
          _key: "cf1ee46862a2",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Arbeidstrening er et tilbud for personer med liten eller mangelfull arbeidserfaring, eller som har fått arbeidsevnen nedsatt og som har behov for arbeidsrettet bistand for å komme i arbeid.",
              _key: "7bdb81860104",
            },
          ],
        },
      ],
      detaljerOgInnhold: [
        {
          _key: "36f4b2cfa769",
          markDefs: [],
          children: [
            {
              _key: "f966704cd495",
              _type: "span",
              marks: [],
              text: "Arbeidstrening vil si å få opplæring i å utføre vanlige arbeidsoppgaver i en begrenset periode. Arbeidstreningen er på en ordinær arbeidsplass, men arbeidsoppgavene er tilrettelagt den enkeltes behov.ggbyy",
            },
          ],
          _type: "block",
          style: "normal",
        },
      ],
    },
    delingMedBruker:
      "Arbeidstrening gir deg trening så du er klar for arbeidslivet.\nOm dette er noe for deg så send meg en melding i Dialogen.",
    regelverkLenker: [
      {
        _id: "123",
        regelverkLenkeNavn: "Regelverk",
        regelverkUrl: "https://www.google.no",
      },
      {
        _id: "1234",
        regelverkLenkeNavn: "Rundskriv",
        regelverkUrl: "https://www.google.no",
      },
    ],
    beskrivelse:
      "Arbeidstrening skal bidra til å styrke tiltaksdeltakers muligheter for å komme i jobb. Arbeidstrening kan ha flere ulike formål i form av arbeidserfaring for å mestre en bestemt jobb, eller behov for en referanse mens man søker ordinært arbeid",
  },
  gruppe_amo: {
    arenakode: TiltakskodeArena.GRUPPEAMO,
    sanityId: "eadeb22c-bd89-4298-a5c2-145f112f8e7d",
    delingMedBruker:
      "Har du vurdert utdanning for å få flere muligheter på arbeidsmarkedet?  \n\nDu kan lese mer om tiltaket på www.nav.no/opplaring \n\nEr dette aktuelt for deg? Gi meg tilbakemelding her i dialogen. \nSvaret ditt vil ikke endre din utbetaling fra NAV. \n\nVi holder kontakten!\nHilsen <Veiledernavn> \n",
    regelverkLenker: [
      {
        _id: "123",
        regelverkLenkeNavn: "Regelverk",
        regelverkUrl: "https://www.google.no",
      },
      {
        _id: "1234",
        regelverkLenkeNavn: "Rundskriv",
        regelverkUrl: "https://www.google.no",
      },
    ],
    faneinnhold: {
      forHvemInfoboks: "Det er viktig at man bruker korrekt innsøkingsprosess for dette tiltaket",
      forHvem: [
        {
          _type: "block",
          style: "normal",
          _key: "965337f85d9a",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Brukere med behov for kvalifisering for å komme inn på arbeidsmarkedet",
              _key: "c51899da33bb",
            },
          ],
          level: 1,
        },
        {
          style: "normal",
          _key: "3c9d2cdb5a6a",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _key: "a5a1dd2fd90e",
              _type: "span",
              marks: [],
              text: "Brukere kan starte i AMO det året de fyller 19 år",
            },
          ],
          level: 1,
          _type: "block",
        },
        {
          style: "normal",
          _key: "a05232acf53d",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Åpent for brukere med situasjonsbestemt, spesielt tilpasset og varig tilpasset innsats",
              _key: "0320c72517e4",
            },
          ],
          level: 1,
          _type: "block",
        },
      ],
      detaljerOgInnhold: [
        {
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Gruppe AMO er for de som er glad i grupper",
              _key: "221dab873e5c",
            },
          ],
          _type: "block",
          style: "normal",
          _key: "f6710efeab29",
        },
      ],
      pameldingOgVarighet: [
        {
          _type: "block",
          style: "normal",
          _key: "4a8d530e6655",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Varighet er spesifisert per gjennomføring",
              _key: "8fa3838ff6b4",
            },
          ],
        },
      ],
    },
    innsatsgrupper: [
      Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
      Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
      Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
      Innsatsgruppe.VARIG_TILPASSET_INNSATS,
    ],
    beskrivelse:
      "Opplæringstiltak i form av Gruppe AMO (jf. tiltaksforskriften § 7-2 a) skal bidra til at arbeidssøkere kvalifiseres til ledige jobber. ",
    navn: "Opplæring - Gruppe AMO",
  },
  avklaring: {
    regelverkLenker: [
      {
        _id: "123",
        regelverkLenkeNavn: "Regelverk",
        regelverkUrl: "https://www.google.no",
      },
      {
        _id: "1234",
        regelverkLenkeNavn: "Rundskriv",
        regelverkUrl: "https://www.google.no",
      },
    ],
    beskrivelse:
      "Avklaring skal kartlegge deltakerens muligheter for arbeid og behov for bistand til å skaffe eller beholde arbeid. Avklaringen skal bidra til at deltaker får økt innsikt i sine muligheter på arbeidsmarkedet og i egne ressurser og ferdigheter i jobbsammenheng.",
    kanKombineresMed: [],
    faneinnhold: {
      forHvemInfoboks: 'Infoboks i fanen "For hvem"',
      pameldingOgVarighet: [
        {
          markDefs: [],
          children: [
            {
              text: "Avklaring kan vare i inntil 4 uker, men med mulighet til forlengelse i ytterligere inntil 8 uker etter en nærmere vurdering ved særlige behov.",
              _key: "0d2b14505742",
              _type: "span",
              marks: [],
            },
          ],
          _type: "block",
          style: "normal",
          _key: "6e17ce995ef3",
        },
        {
          _key: "d3b0d033665d",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "\nTiltaket gjennomføres som et heltidstilbud med 30 timer pr uke, 5 virkedager pr uke. ",
              _key: "665002270096",
            },
          ],
          _type: "block",
          style: "normal",
        },
        {
          style: "normal",
          _key: "ea54a5378f03",
          markDefs: [],
          children: [
            {
              _key: "fbd2ae7b643c",
              _type: "span",
              marks: [],
              text: "",
            },
          ],
          _type: "block",
        },
        {
          children: [
            {
              _type: "span",
              marks: [],
              text: "Omfanget skal være individuelt tilpasset til den enkelte deltakers behov.",
              _key: "cb202bee371c",
            },
          ],
          _type: "block",
          style: "normal",
          _key: "d8271280b026",
          markDefs: [],
        },
      ],
      forHvem: [
        {
          children: [
            {
              _type: "span",
              marks: ["strong"],
              text: "Hvem kan delta:",
              _key: "2b4e3e471514",
            },
          ],
          _type: "block",
          style: "normal",
          _key: "8caa6fa462f4",
          markDefs: [],
        },
        {
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Personer som har behov for kartlegging av fremtidig arbeidsmuligheter",
              _key: "6913e21d9f0b",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
          _key: "799b5d1b6883",
          listItem: "bullet",
        },
        {
          _key: "6fa666901dca",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              marks: [],
              text: "Åpen for brukere fra alle innsatsgrupper unntatt brukere med standard innsats",
              _key: "101e0a133499",
              _type: "span",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
        },
        {
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "For sykmeldte arbeidstakere er det ikke nødvendig med behovs-/arbeidsevnevurdering og 14a-vedtak.\n",
              _key: "80891180d397",
            },
          ],
          _type: "block",
          style: "normal",
          _key: "c6a9d5736bad",
        },
      ],
      detaljerOgInnhold: [
        {
          children: [
            {
              text: "Kurset inneholder:",
              _key: "9b532cf75fc0",
              _type: "span",
              marks: ["strong"],
            },
          ],
          _type: "block",
          style: "normal",
          _key: "020afe77d2d8",
          markDefs: [],
        },
        {
          _type: "block",
          style: "normal",
          _key: "bf909fd8a322",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _key: "77322d16a1ef",
              _type: "span",
              marks: [],
              text: "Kartlegge hvordan faglig og sosial kompetanse, helse, ferdigheter, evner og interesser samt andre forhold påvirker muligheten for å skaffe eller beholde arbeid",
            },
          ],
          level: 1,
        },
        {
          children: [
            {
              text: "Kartlegging av hvilken type tilrettelegging og individuell bistand deltaker trenger for å stå i et arbeid",
              _key: "643a29221ff2",
              _type: "span",
              marks: [],
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
          _key: "ec499aa64d19",
          listItem: "bullet",
          markDefs: [],
        },
        {
          _key: "9d3672c598c3",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              marks: [],
              text: "Kartlegging av basiskompetanse/grunnleggende ferdigheter",
              _key: "3870eb7699e8",
              _type: "span",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
        },
        {
          _key: "f7467a7d69f7",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Informasjon og økt kunnskap om arbeidsmarked, yrker og jobbkrav",
              _key: "b91cd22a0bbc",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
        },
        {
          children: [
            {
              _type: "span",
              marks: [],
              text: "Karriereveiledning for valg av realistiske yrkesmål og arbeidsoppgaver",
              _key: "e382f83ea7f6",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
          _key: "ca22c21c2aa4",
          listItem: "bullet",
          markDefs: [],
        },
        {
          _key: "8a1916f627f7",
          listItem: "bullet",
          markDefs: [],
          children: [
            {
              _type: "span",
              marks: [],
              text: "Motivasjons- og mestringsaktiviteter som skal bidra til at deltaker utarbeider egne målsetninger for å kunne skaffe eller beholde arbeid",
              _key: "e5f66eef0577",
            },
          ],
          level: 1,
          _type: "block",
          style: "normal",
        },
        {
          _type: "block",
          style: "normal",
          _key: "5995993c0a33",
          markDefs: [],
          children: [
            {
              marks: ["strong"],
              text: "Tiltaket skal ikke benyttes for å kartlegge behovet til bruker for full uførepensjon. ",
              _key: "48d45145ac2f",
              _type: "span",
            },
          ],
        },
      ],
    },
    innsatsgrupper: [
      Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
      Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
      Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
      Innsatsgruppe.VARIG_TILPASSET_INNSATS,
    ],
    sanityId: "f9618e97-4510-49e2-b748-29cae84d9019",
    delingMedBruker:
      "Vi har nå et arbeidsmarkedstiltak som jeg tenker kan passe deg godt. Det heter <tiltaksnavn>.\n\nI tiltaket kan det være aktuelt å kartlegge og gi hjelp til å\n- tilpasse arbeidssituasjonen og -oppgaver slik at du kan utføre jobben\n- finne ut hva slags hjelp eller tilrettelegging som skal til for at du kan jobbe\n- se kompetansen din og mulighetene dine\n\nUnder avklaring kan du også få\n- informasjon om arbeidsmarkedet, yrker og jobbkrav\n- veiledning for å velge yrkesmål og arbeidsoppgaver\n- arbeidsutprøving på en arbeidsplass\n\nDu kan lese mer om kurset på www.nav.no/avklaring ",
    navn: "Avklaring",
  },
};
