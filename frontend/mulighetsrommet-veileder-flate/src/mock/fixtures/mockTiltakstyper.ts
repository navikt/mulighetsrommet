import { Innsatsgruppe, Tiltakskode, VeilederflateTiltakstype } from "@api-client";

export const mockTiltakstyper: { [name: string]: VeilederflateTiltakstype } = {
  VTA: {
    id: "02509279-0a0f-4bd6-b506-f40111e4ba14",
    arenakode: null,
    tiltakskode: Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
    tiltaksgruppe: null,
    features: [],
    egenskaper: [],
    regelverkLenker: [],
    faglenker: [
      {
        id: "db0ea48f-cb63-416f-aa4d-1b210baec6bb",
        navn: "Regelverk",
        url: "https://www.google.no",
        beskrivelse: null,
      },
      {
        id: "22f5ecc0-b8a1-4bd7-8e5d-153a33140181",
        navn: "Rundskriv",
        url: "https://www.google.no",
        beskrivelse: null,
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
      forHvemInfoboks: null,
      kontaktinfo: null,
      kontaktinfoInfoboks: null,
      detaljerOgInnholdInfoboks: null,
      oppskrift: null,
      pameldingOgVarighet: null,
      pameldingOgVarighetInfoboks: null,
      delMedBruker: null,
      lenker: null,
    },
    delingMedBruker:
      "Vi har nå et arbeidsmarkedstiltak som jeg tenker kan passe deg godt. Det heter <tiltaksnavn> og  er et tilbud for deg som får uføretrygd. Du jobber i en skjermet bedrift med arbeidsoppgaver som er tilpasset deg. Du kan også jobbe i en ordinær bedrift.\n\nDu kan lese mer om tiltaket på www.nav.no/varig-tilrettelagt-arbeid",
    beskrivelse:
      "Varig tilrettelagt arbeid (VTA-S) skal gi brukeren arbeid med oppgaver tilpasset han eller hennes arbeidsevne, og tilby et individuelt tilpasset opplegg.",
    navn: "VTA - Varig tilrettelagt arbeid",
    innsatsgrupper: [Innsatsgruppe.JOBBE_DELVIS, Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE],
    kanKombineresMed: [],
  },
  OpplaringEnkeltplassFagOgYrke: {
    id: "222a0065-9777-4e09-b2cf-4f48759f86e3",
    beskrivelse:
      "Opplæringstiltak i form av Fag- og yrkesopplæring (jf. tiltaksforskriften § 7-2 b) er opplæring som fører frem mot fag- eller svennebrev, praksisbrev eller kompetansebevis som lærekandidat. \n\nOpplæringen skal bidra til at arbeidssøkere kvalifiseres til ledige jobber. ",
    navn: "Opplæring - Enkeltplass Fag- og yrkesopplæring",
    regelverkLenker: [],
    faglenker: null,
    delingMedBruker: null,
    arenakode: null,
    tiltakskode: Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING,
    tiltaksgruppe: "Opplæring",
    features: [],
    egenskaper: [],
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
          kanKombineresMed: [],
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
              text: "Husk å følge rutiner for anskaffelse av enkeltplasser hvis Nav skal betale for en skoleplass for en bruker.",
              _key: "716ab73fbda40",
            },
          ],
          _type: "block",
        },
      ],
      forHvemInfoboks: null,
      kontaktinfo: null,
      kontaktinfoInfoboks: null,
      detaljerOgInnholdInfoboks: null,
      oppskrift: null,
      pameldingOgVarighetInfoboks: null,
      delMedBruker: null,
      lenker: null,
    },
    innsatsgrupper: [
      Innsatsgruppe.TRENGER_VEILEDNING,
      Innsatsgruppe.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE,
      Innsatsgruppe.JOBBE_DELVIS,
      Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
    ],
    kanKombineresMed: [],
  },
  jobbklubb: {
    id: "31e72dd8-ad05-4e81-a7f9-fd4c8f295864",
    tiltakskode: Tiltakskode.JOBBKLUBB,
    tiltaksgruppe: null,
    features: [],
    egenskaper: [],
    delingMedBruker:
      "Jobbklubb er et kortvarig tiltak for deg som søker jobb. Når du deltar på jobbklubb, får du støtte og hjelp til å orientere deg på arbeidsmarkedet og være en aktiv jobbsøker.\n\nDu kan lese mer om kurset på www.nav.no/jobbklubb",
    regelverkLenker: [],
    faglenker: [
      {
        id: "db0ea48f-cb63-416f-aa4d-1b210baec6bb",
        navn: "Regelverk",
        url: "https://www.google.no",
        beskrivelse: null,
      },
      {
        id: "22f5ecc0-b8a1-4bd7-8e5d-153a33140181",
        navn: "Rundskriv",
        url: "https://www.google.no",
        beskrivelse: null,
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
              text: "Digitalt jobbsøkerkurs bør også vurderes før bruker søkes inn på ordinær jobbklubb. Det gjelder ikke for Øst-Viken, Vest-Viken eller Innlandet.",
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
      forHvemInfoboks: null,
      kontaktinfo: null,
      kontaktinfoInfoboks: null,
      detaljerOgInnholdInfoboks: null,
      oppskrift: null,
      pameldingOgVarighetInfoboks: null,
      delMedBruker: null,
      lenker: null,
    },
    innsatsgrupper: [
      Innsatsgruppe.GODE_MULIGHETER,
      Innsatsgruppe.TRENGER_VEILEDNING,
      Innsatsgruppe.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE,
      Innsatsgruppe.JOBBE_DELVIS,
      Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
    ],
    beskrivelse:
      "Jobbklubb er et kortvarig tiltak for de som søker jobb. Ved deltagelse på jobbklubb får brukeren støtte og hjelp til å orientere seg på arbeidsmarkedet og være en aktiv jobbsøker.",
    navn: "Jobbklubb",
    kanKombineresMed: [],
    arenakode: null,
  },
  oppfolging: {
    tiltaksgruppe: null,
    features: [],
    egenskaper: [],
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
              text: "\nVarigheten av oppfølgingen skal tilpasses deltakerens individuelle behov. Timeforbruket vil derfor variere fra deltaker til deltaker. Nav antar at et gjennomsnittsbehov vil være om lag 10 timer oppfølging per deltaker per måned i hele tiltaksperioden. Det understrekes imidlertid at hver enkelt deltaker skal få det antallet oppfølgingstimer som er nødvendig for at deltaker skal nå målet om å få og/eller beholde arbeid. ",
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
              text: "Se også Digitalt jobbsøkerkurs for arbeidsledige (Digital jobbklubb)",
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
      forHvemInfoboks: null,
      kontaktinfo: null,
      kontaktinfoInfoboks: null,
      detaljerOgInnholdInfoboks: null,
      oppskrift: null,
      pameldingOgVarighetInfoboks: null,
      delMedBruker: null,
      lenker: null,
    },
    innsatsgrupper: [
      Innsatsgruppe.TRENGER_VEILEDNING,
      Innsatsgruppe.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE,
      Innsatsgruppe.JOBBE_DELVIS,
      Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
    ],
    delingMedBruker:
      "Vi har nå et arbeidsmarkedstiltak som jeg tenker kan passe deg godt. Det heter <tiltaksnavn>  og vil tilpasses dine behov for støtte for å skaffe eller beholde en jobb.\n\nDu kan blant annet få veiledning, råd og bistand til\n- jobbsøking og karriereveiledning\n- tilpasning og tilrettelegging av arbeidet og arbeidssituasjonen\n- praktiske oppgaver knyttet til arbeid\n- opplæring i sosiale og arbeidsrelaterte ferdigheter i arbeidslivet\n\nDu kan lese mer om kurset på www.nav.no/oppfolging",
    navn: "Oppfølging",
    id: "5ac48c03-1f4c-4d4b-b862-050caca92080",
    regelverkLenker: [],
    faglenker: [
      {
        id: "db0ea48f-cb63-416f-aa4d-1b210baec6bb",
        navn: "Regelverk",
        url: "https://www.google.no",
        beskrivelse: null,
      },
      {
        id: "22f5ecc0-b8a1-4bd7-8e5d-153a33140181",
        navn: "Rundskriv",
        url: "https://www.google.no",
        beskrivelse: null,
      },
    ],
    beskrivelse:
      "Oppfølging skal gi bistand med sikte på at tiltaksdeltakere skaffer seg og/eller beholder lønnet arbeid. Målet er at tiltaksdeltaker i størst mulig grad skal bli selvforsørget med en varig tilknytning til arbeidslivet. ",
    kanKombineresMed: [],
    arenakode: null,
    tiltakskode: Tiltakskode.OPPFOLGING,
  },
  grufagyrke: {
    tiltaksgruppe: "Opplæring",
    features: [],
    egenskaper: [],
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
      forHvemInfoboks: null,
      kontaktinfo: null,
      kontaktinfoInfoboks: null,
      detaljerOgInnhold: null,
      detaljerOgInnholdInfoboks: null,
      oppskrift: null,
      pameldingOgVarighet: null,
      pameldingOgVarighetInfoboks: null,
      delMedBruker: null,
      lenker: null,
    },
    innsatsgrupper: [
      Innsatsgruppe.TRENGER_VEILEDNING,
      Innsatsgruppe.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE,
      Innsatsgruppe.JOBBE_DELVIS,
      Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
    ],
    delingMedBruker:
      "Vi har nå et opplæringstiltak som jeg tenker kan passe deg godt. Det heter <tiltaksnavn>, og hensikten med tiltaket er å kvalifisere deg for ledige jobber.\n\nDu kan lese mer om tiltaket på www.nav.no/opplaring",
    navn: "Fag- og yrkesopplæring (gruppe)",
    id: "7f353dcd-37c2-42f8-bab6-ac2a60669839",
    beskrivelse:
      "Opplæringstiltak i form av Gruppe Fag- og yrkesopplæring (jf. tiltaksforskriften § 7-2 b) skal bidra til at arbeidssøkere kvalifiseres til ledige jobber. ",
    kanKombineresMed: [],
    regelverkLenker: [],
    faglenker: null,
    arenakode: null,
    tiltakskode: Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
  },
  mentor: {
    tiltaksgruppe: null,
    features: [],
    egenskaper: [],
    arenakode: "MENTOR",
    faneinnhold: {
      pameldingOgVarighet: [
        {
          markDefs: [],
          children: [
            {
              text: "Påmelding til mentor er tilgjengelig som digital avtale. Det vil si at deltaker, arbeidsgiver og Nav fyller ut, ser over og godkjenner avtalen i samme løsning i sanntid. Mentoren er ikke en part i avtalen, men har lesetilgang til det meste i avtalen og må signere en taushetserklæring.",
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
      forHvemInfoboks: null,
      kontaktinfo: null,
      kontaktinfoInfoboks: null,
      detaljerOgInnholdInfoboks: null,
      oppskrift: null,
      pameldingOgVarighetInfoboks: null,
      delMedBruker: null,
      lenker: null,
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
      Innsatsgruppe.TRENGER_VEILEDNING,
      Innsatsgruppe.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE,
      Innsatsgruppe.JOBBE_DELVIS,
      Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
    ],
    id: "ad998fc6-310e-45d4-a056-57732fed87b4",
    regelverkLenker: [],
    faglenker: [
      {
        id: "db0ea48f-cb63-416f-aa4d-1b210baec6bb",
        navn: "Regelverk",
        url: "https://www.google.no",
        beskrivelse: null,
      },
      {
        id: "22f5ecc0-b8a1-4bd7-8e5d-153a33140181",
        navn: "Rundskriv",
        url: "https://www.google.no",
        beskrivelse: null,
      },
    ],
    kanKombineresMed: [
      { id: "a1000000-0000-0000-0000-000000000001", navn: "Arbeidstrening" },
      {
        id: "bbb8d042-b30e-4e4a-8cd0-210019b19de3",
        navn: "Arbeidsmarkedsopplæring (enkeltplass)",
      },
      {
        id: "eadeb22c-bd89-4298-a5c2-145f112f8e7d",
        navn: "Arbeidsmarkedsopplæring (gruppe)",
      },
      {
        id: "222a0065-9777-4e09-b2cf-4f48759f86e3",
        navn: "Fag- og yrkesopplæring eller fagskole (enkeltplass)",
      },
      {
        id: "7f353dcd-37c2-42f8-bab6-ac2a60669839",
        navn: "Fag- og yrkesopplæring (gruppe)",
      },
      { id: "a1000000-0000-0000-0000-000000000002", navn: "Høyere utdanning" },
      { id: "a1000000-0000-0000-0000-000000000003", navn: "Tilskudd til sommerjobb" },
      { id: "a1000000-0000-0000-0000-000000000004", navn: "Midlertidig lønnstilskudd" },
      { id: "a1000000-0000-0000-0000-000000000005", navn: "Varig lønnstilskudd" },
      { id: "a1000000-0000-0000-0000-000000000006", navn: "Inkluderingstilskudd" },
      { id: "a1000000-0000-0000-0000-000000000007", navn: "IPS (individuell jobbstøtte)" },
      { id: "a1000000-0000-0000-0000-000000000008", navn: "IPS (ung)" },
    ],
    tiltakskode: Tiltakskode.MENTOR,
  },
  enkeltplass_amo: {
    tiltaksgruppe: "Opplæring",
    features: [],
    egenskaper: [],
    innsatsgrupper: [
      Innsatsgruppe.TRENGER_VEILEDNING,
      Innsatsgruppe.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE,
      Innsatsgruppe.JOBBE_DELVIS,
      Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
    ],
    id: "bbb8d042-b30e-4e4a-8cd0-210019b19de3",
    regelverkLenker: [],
    faglenker: [
      {
        id: "db0ea48f-cb63-416f-aa4d-1b210baec6bb",
        navn: "Regelverk",
        url: "https://www.google.no",
        beskrivelse: null,
      },
      {
        id: "22f5ecc0-b8a1-4bd7-8e5d-153a33140181",
        navn: "Rundskriv",
        url: "https://www.google.no",
        beskrivelse: null,
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
      forHvemInfoboks: null,
      kontaktinfo: null,
      kontaktinfoInfoboks: null,
      detaljerOgInnholdInfoboks: null,
      oppskrift: null,
      pameldingOgVarighetInfoboks: null,
      delMedBruker: null,
      lenker: null,
    },
    beskrivelse:
      "Opplæringstiltak i form av Enkeltplass AMO (jf. tiltaksforskriften § 7-2 a) består av kortvarige kurs basert på behov i arbeidsmarkedet, og skal bidra til at arbeidssøkere kvalifiseres til ledige jobber.",
    navn: "Opplæring - Enkeltplass AMO",
    kanKombineresMed: [],
    delingMedBruker: null,
    arenakode: null,
    tiltakskode: Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
  },
  AFT: {
    tiltaksgruppe: null,
    features: [],
    egenskaper: [],
    beskrivelse:
      "AFT er et tiltak som skal bidra til å prøve ut tiltaksdeltakerens arbeidsevne og til å styrke mulighetene for å få ordinært arbeid. Det er et tiltak med individuell tilrettelegging som kan tilbys personer med sammensatte bistandsbehov som har fått sin arbeidsevne nedsatt og som har særlig usikre yrkesmessige forutsetninger.",

    regelverkLenker: [],
    faglenker: [
      {
        id: "db0ea48f-cb63-416f-aa4d-1b210baec6bb",
        navn: "Regelverk",
        url: "https://www.google.no",
        beskrivelse: null,
      },
      {
        id: "22f5ecc0-b8a1-4bd7-8e5d-153a33140181",
        navn: "Rundskriv",
        url: "https://www.google.no",
        beskrivelse: null,
      },
    ],
    id: "d03363e0-7d46-411b-aec4-fb9449e30eb8",
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
      forHvemInfoboks: null,
      kontaktinfo: null,
      kontaktinfoInfoboks: null,
      detaljerOgInnholdInfoboks: null,
      oppskrift: null,
      pameldingOgVarighetInfoboks: null,
      delMedBruker: null,
      lenker: null,
    },
    innsatsgrupper: [
      Innsatsgruppe.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE,
      Innsatsgruppe.JOBBE_DELVIS,
      Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
    ],
    delingMedBruker:
      "Vi har nå et arbeidsmarkedstiltak som jeg tenker kan passe deg godt. Det heter <tiltaksnavn> og er et tilbud for deg som deg som har nedsatt arbeidsevne og trenger hjelp for å komme i jobb.\n\nMålet er å avklare arbeidsevnen din gjennom arbeidstrening i ulike arbeidssituasjoner.\n\n- Etter en periode med forberedende arbeidstrening i et tilrettelagt arbeidsmiljø får du arbeidstrening i en vanlig bedrift.\n- Du får kartlagt kompetansen din og får karriereveiledning.\n- Du kan få tilrettelagt opplæring hvis du ønsker å gå videre med et yrkesfaglig utdanningsløp. Opplæringen skal bedre mulighetene dine til å komme i jobb.\n\nDu kan lese mer om kurset på www.nav.no/arbeidsforberedende-trening",
    kanKombineresMed: [],
    arenakode: null,
    tiltakskode: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
  },
  gruppe_amo: {
    tiltaksgruppe: "Opplæring",
    features: [],
    egenskaper: [],
    arenakode: "GRUPPEAMO",
    tiltakskode: Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
    id: "eadeb22c-bd89-4298-a5c2-145f112f8e7d",
    delingMedBruker:
      "Har du vurdert utdanning for å få flere muligheter på arbeidsmarkedet?  \n\nDu kan lese mer om tiltaket på www.nav.no/opplaring \n\nEr dette aktuelt for deg? Gi meg tilbakemelding her i dialogen. \nSvaret ditt vil ikke endre din utbetaling fra Nav. \n\nVi holder kontakten!\nHilsen <Veiledernavn> \n",
    regelverkLenker: [],
    faglenker: [
      {
        id: "db0ea48f-cb63-416f-aa4d-1b210baec6bb",
        navn: "Regelverk",
        url: "https://www.google.no",
        beskrivelse: null,
      },
      {
        id: "22f5ecc0-b8a1-4bd7-8e5d-153a33140181",
        navn: "Rundskriv",
        url: "https://www.google.no",
        beskrivelse: null,
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
      kontaktinfo: null,
      kontaktinfoInfoboks: null,
      detaljerOgInnholdInfoboks: null,
      oppskrift: null,
      pameldingOgVarighetInfoboks: null,
      delMedBruker: null,
      lenker: null,
    },
    innsatsgrupper: [
      Innsatsgruppe.TRENGER_VEILEDNING,
      Innsatsgruppe.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE,
      Innsatsgruppe.JOBBE_DELVIS,
      Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
    ],
    beskrivelse:
      "Opplæringstiltak i form av Gruppe AMO (jf. tiltaksforskriften § 7-2 a) skal bidra til at arbeidssøkere kvalifiseres til ledige jobber. ",
    navn: "Arbeidsmarkedsopplæring (gruppe)",
    kanKombineresMed: [],
  },
  avklaring: {
    tiltaksgruppe: null,
    features: [],
    egenskaper: [],
    arenakode: "AVKLARAG",
    regelverkLenker: [],
    faglenker: [
      {
        id: "db0ea48f-cb63-416f-aa4d-1b210baec6bb",
        navn: "Regelverk",
        url: "https://www.google.no",
        beskrivelse: null,
      },
      {
        id: "22f5ecc0-b8a1-4bd7-8e5d-153a33140181",
        navn: "Rundskriv",
        url: "https://www.google.no",
        beskrivelse: null,
      },
    ],
    beskrivelse:
      "Avklaring skal kartlegge deltakerens muligheter for arbeid og behov for bistand til å skaffe eller beholde arbeid. Avklaringen skal bidra til at deltaker får økt innsikt i sine muligheter på arbeidsmarkedet og i egne ressurser og ferdigheter i jobbsammenheng.",
    kanKombineresMed: [
      { id: "a1000000-0000-0000-0000-000000000004", navn: "Midlertidig lønnstilskudd" },
      { id: "a1000000-0000-0000-0000-000000000006", navn: "Inkluderingstilskudd" },
    ],
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
      kontaktinfo: null,
      kontaktinfoInfoboks: null,
      detaljerOgInnholdInfoboks: null,
      oppskrift: null,
      pameldingOgVarighetInfoboks: null,
      delMedBruker: null,
      lenker: null,
    },
    innsatsgrupper: [
      Innsatsgruppe.TRENGER_VEILEDNING,
      Innsatsgruppe.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE,
      Innsatsgruppe.JOBBE_DELVIS,
      Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
    ],
    id: "f9618e97-4510-49e2-b748-29cae84d9019",
    delingMedBruker:
      "Vi har nå et arbeidsmarkedstiltak som jeg tenker kan passe deg godt. Det heter <tiltaksnavn>.\n\nI tiltaket kan det være aktuelt å kartlegge og gi hjelp til å\n- tilpasse arbeidssituasjonen og -oppgaver slik at du kan utføre jobben\n- finne ut hva slags hjelp eller tilrettelegging som skal til for at du kan jobbe\n- se kompetansen din og mulighetene dine\n\nUnder avklaring kan du også få\n- informasjon om arbeidsmarkedet, yrker og jobbkrav\n- veiledning for å velge yrkesmål og arbeidsoppgaver\n- arbeidsutprøving på en arbeidsplass\n\n[Du kan lese mer om kurset på Nav.no](www.nav.no/avklaring)",
    navn: "Avklaring",
    tiltakskode: Tiltakskode.AVKLARING,
  },
};
