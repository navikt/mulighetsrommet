import {
  Innsatsgruppe,
  PaginertTiltakstype,
  Tiltakskode,
  Tiltakstypestatus,
  VeilederflateInnsatsgruppe,
  VeilederflateTiltakstype,
} from "mulighetsrommet-api-client";

export const mockTiltakstyper = {
  ARBFORB: {
    id: "59a64a02-efdd-471d-9529-356ff5553a5d",
    navn: "Arbeidsforberedende trening (AFT)",
    arenaKode: Tiltakskode.ARBFORB,
    fraDato: "2016-01-01",
    tilDato: "2099-01-01",
    status: Tiltakstypestatus.AKTIV,
    sanityId: "d03363e0-7d46-411b-aec4-fb9449e30eb8",
  },
  ARBRRHDAG: {
    id: "1d5bf722-02aa-4aa5-97e2-f359ea307a14",
    navn: "Arbeidsrettet rehabilitering (dag)",
    arenaKode: Tiltakskode.ARBRRHDAG,
    fraDato: "2012-01-01",
    tilDato: "2099-01-01",
    status: Tiltakstypestatus.AKTIV,
    sanityId: "29c3d3cb-ffbf-4c22-8ffc-fea5d7f6c822",
  },
  AVKLARAG: {
    id: "938c2e7b-91d6-4eee-97d3-a110ccbc5968",
    navn: "Avklaring",
    arenaKode: Tiltakskode.AVKLARAG,
    fraDato: "2009-01-01",
    tilDato: "2099-01-01",
    status: Tiltakstypestatus.AKTIV,
    sanityId: "f9618e97-4510-49e2-b748-29cae84d9019",
  },
  DIGIOPPARB: {
    id: "e47447e3-bbe4-4c41-839d-f352130b2e8a",
    navn: "Digitalt oppfølgingstiltak for arbeidsledige (jobbklubb)",
    arenaKode: Tiltakskode.DIGIOPPARB,
    fraDato: "2021-01-01",
    tilDato: "2099-01-01",
    status: Tiltakstypestatus.AKTIV,
    sanityId: "3526de0d-ad4c-4b81-b072-a13b3a4b4ed3",
  },
  GRUPPEAMO: {
    id: "9b52265c-914c-413d-bca4-e9d7b3f1bd8d",
    navn: "Gruppe AMO",
    arenaKode: Tiltakskode.GRUPPEAMO,
    fraDato: "2019-07-01",
    tilDato: "2099-01-01",
    status: Tiltakstypestatus.AKTIV,
    sanityId: "eadeb22c-bd89-4298-a5c2-145f112f8e7d",
  },
  GRUFAGYRKE: {
    id: "53ecc473-c0ce-40ea-88c3-9f4a3131080b",
    navn: "Gruppe Fag- og yrkesopplæring VGS og høyere yrkesfaglig utdanning",
    arenaKode: Tiltakskode.GRUFAGYRKE,
    fraDato: "2019-07-01",
    tilDato: "2099-01-01",
    status: Tiltakstypestatus.AKTIV,
    sanityId: "7f353dcd-37c2-42f8-bab6-ac2a60669839",
  },
  JOBBK: {
    id: "95766f55-a456-4c4b-9a77-ca61ae214409",
    navn: "Jobbklubb",
    arenaKode: Tiltakskode.JOBBK,
    fraDato: "2003-10-10",
    tilDato: "2099-01-01",
    status: Tiltakstypestatus.AKTIV,
    sanityId: "31e72dd8-ad05-4e81-a7f9-fd4c8f295864",
  },
  INDOPPFAG: {
    id: "71a51692-35c5-4951-84eb-a338b0a57210",
    navn: "Oppfølging",
    arenaKode: Tiltakskode.INDOPPFAG,
    fraDato: "2009-01-01",
    tilDato: "2099-01-01",
    status: Tiltakstypestatus.AKTIV,
    sanityId: "5ac48c03-1f4c-4d4b-b862-050caca92080",
  },
  VASV: {
    id: "6fb921d6-0a87-4b8a-82a4-067477c1e113",
    navn: "Varig tilrettelagt arbeid i skjermet virksomhet",
    arenaKode: Tiltakskode.VASV,
    fraDato: "2001-01-01",
    tilDato: "2099-01-01",
    status: Tiltakstypestatus.AKTIV,
    sanityId: "02509279-0a0f-4bd6-b506-f40111e4ba14",
  },
};

export const paginertMockTiltakstyper: PaginertTiltakstype = {
  pagination: {
    totalCount: Object.values(mockTiltakstyper).length,
    currentPage: 1,
    pageSize: 50,
  },
  data: Object.values(mockTiltakstyper),
};

export const mockVeilederflateTiltakstypeAFT: VeilederflateTiltakstype = {
  oppskrifter: [
    {
      _updatedAt: new Date().toDateString(),
      _id: "69ed191f-abc6-404e-b494-2316c96cecfb",
      navn: "Tiltaksregistrering i Arena for tiltak med automatisk henvisning",
      beskrivelse:
        "Gjelder tiltakene Arbeidsrettet rehabilitering (ARR), Arbeidsforberedende trening (AFT), Avklaring, Oppfølging og Varig tilrettelagt arbeid i skjermet virksomhet (VTA)\u000b",
      steg: [
        {
          _type: "steg",
          navn: "Vurdere tiltaksbehov",
          innhold: [
            {
              _key: "8b809dbd9df1",
              markDefs: [],
              children: [
                {
                  _type: "span",
                  marks: [],
                  text: "For disse tiltakene tar du utgangspunkt i den aktuelle deltakeren. ",
                  _key: "c3609b447add0",
                },
              ],
              _type: "block",
              style: "normal",
            },
            {
              children: [
                {
                  marks: [],
                  text: "Klikk på ikonet Søk person og legg inn fødsels- og personnummer.",
                  _key: "2fdfbd5eef0b",
                  _type: "span",
                },
              ],
              _type: "block",
              style: "normal",
              _key: "36e692ceb33d",
              markDefs: [],
            },
            {
              children: [
                {
                  _type: "span",
                  marks: [],
                  text: "",
                  _key: "7c9217bceed3",
                },
              ],
              _type: "block",
              style: "normal",
              _key: "c71627cda6d4",
              markDefs: [],
            },
            {
              _key: "baf1957cbf55",
              markDefs: [],
              children: [
                {
                  text: "",
                  _key: "c88ed5cc8d99",
                  _type: "span",
                  marks: [],
                },
              ],
              _type: "block",
              style: "normal",
            },
            {
              _type: "tips",
              innhold: [
                {
                  _type: "block",
                  style: "normal",
                  _key: "a4ae91d29e1a",
                  markDefs: [],
                  children: [
                    {
                      _type: "span",
                      marks: [],
                      text: "Her kan det være tips",
                      _key: "9ce39a23866f",
                    },
                  ],
                },
              ],
              _key: "08f1e55c8c18",
            },
            {
              children: [
                {
                  marks: [],
                  text: "",
                  _key: "67bc816fdfef",
                  _type: "span",
                },
              ],
              _type: "block",
              style: "normal",
              _key: "ca6ca6ed6a53",
              markDefs: [],
            },
            {
              _key: "a450ca301b93",
              asset: {
                url: "https://cdn.sanity.io/images/xegcworx/test/d87db29dc697194d8e914f2b364ba9e2bb4905f7-362x172.jpg",
                assetId: "d87db29dc697194d8e914f2b364ba9e2bb4905f7",
                _createdAt: "2023-11-21T14:17:54Z",
                originalFilename: "steg1.jpg",
                mimeType: "image/jpeg",
                metadata: {
                  hasAlpha: false,
                  lqip: "data:image/jpeg;base64,/9j/2wBDAAYEBQYFBAYGBQYHBwYIChAKCgkJChQODwwQFxQYGBcUFhYaHSUfGhsjHBYWICwgIyYnKSopGR8tMC0oMCUoKSj/2wBDAQcHBwoIChMKChMoGhYaKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCj/wAARCAAKABQDASIAAhEBAxEB/8QAGgAAAQUBAAAAAAAAAAAAAAAAAAECAwUGB//EACMQAAICAQMDBQAAAAAAAAAAAAECBBEAAwUGEhMxFSEyUZH/xAAWAQEBAQAAAAAAAAAAAAAAAAAFAwT/xAAiEQAABQIHAQAAAAAAAAAAAAAAAQIEEQVhFRZRU5Gi4cH/2gAMAwEAAhEDEQA/AGvx7a58ytSUHkO1fE3kO4cb2aDqBJsldJyLVWB8Zt+2iy7VFB6vIGc85A7am6yO4zNTkDqN1k3T1beIk5uEaXSyfmpJqiLT9IJ6Vxz3qbp/hwyuofQwzLi69D58DGVk7nX0f//Z",
                  dimensions: {
                    _type: "sanity.imageDimensions",
                    width: 362,
                    aspectRatio: 2.104651162790698,
                    height: 172,
                  },
                  isOpaque: true,
                  blurHash: "MBH-r+Lz9^rY-o0O:6=xpINbUbGto}#,w^",
                  _type: "sanity.imageMetadata",
                  palette: {
                    dominant: {
                      background: "#0a205f",
                      _type: "sanity.imagePaletteSwatch",
                      foreground: "#fff",
                      title: "#fff",
                      population: 9.31,
                    },
                    _type: "sanity.imagePalette",
                    darkMuted: {
                      population: 8.1,
                      background: "#233451",
                      _type: "sanity.imagePaletteSwatch",
                      foreground: "#fff",
                      title: "#fff",
                    },
                    muted: {
                      background: "#5f6d92",
                      _type: "sanity.imagePaletteSwatch",
                      foreground: "#fff",
                      title: "#fff",
                      population: 3.28,
                    },
                    lightVibrant: {
                      foreground: "#000",
                      title: "#fff",
                      population: 0.61,
                      background: "#f19481",
                      _type: "sanity.imagePaletteSwatch",
                    },
                    darkVibrant: {
                      population: 9.31,
                      background: "#0a205f",
                      _type: "sanity.imagePaletteSwatch",
                      foreground: "#fff",
                      title: "#fff",
                    },
                    lightMuted: {
                      background: "#cfacc4",
                      _type: "sanity.imagePaletteSwatch",
                      foreground: "#000",
                      title: "#fff",
                      population: 0.05,
                    },
                    vibrant: {
                      title: "#fff",
                      population: 7.85,
                      background: "#f92907",
                      _type: "sanity.imagePaletteSwatch",
                      foreground: "#fff",
                    },
                  },
                },
                _rev: "A48zhbJhtmHRCPZnQUSb8m",
                _type: "sanity.imageAsset",
                extension: "jpg",
                sha1hash: "d87db29dc697194d8e914f2b364ba9e2bb4905f7",
                _id: "image-d87db29dc697194d8e914f2b364ba9e2bb4905f7-362x172-jpg",
                uploadId: "Iikl07Y69Q9OjdqahQ2t2luY5FhnMDiQ",
                size: 28833,
                _updatedAt: "2023-11-21T14:17:54Z",
                path: "images/xegcworx/test/d87db29dc697194d8e914f2b364ba9e2bb4905f7-362x172.jpg",
              },
              altText: "Et skjermbilde fra Arena med fokus på å søke inn person.",
              _type: "image",
            },
            {
              _type: "block",
              style: "normal",
              _key: "488d437f8b93",
              markDefs: [],
              children: [
                {
                  _type: "span",
                  marks: [],
                  text: "Start ny oppgave på personen. Velg oppgavetype Vurder tiltaksbehov, og klikk Ok.",
                  _key: "9a0c114875120",
                },
              ],
            },
            {
              markDefs: [],
              children: [
                {
                  _key: "ec935d57f1aa",
                  _type: "span",
                  marks: [],
                  text: "",
                },
              ],
              _type: "block",
              style: "normal",
              _key: "8369290c5261",
            },
            {
              altText: "Skjermbilde fra Arena som viser vurdering av tiltaksbehov",
              _type: "image",
              _key: "8f40713a84f3",
              asset: {
                path: "images/xegcworx/test/b738ea7c5bb1fa7b0aeaae8c540bb3e6dee36fd2-412x286.jpg",
                size: 31678,
                _id: "image-b738ea7c5bb1fa7b0aeaae8c540bb3e6dee36fd2-412x286-jpg",
                _updatedAt: "2023-11-21T14:18:24Z",
                sha1hash: "b738ea7c5bb1fa7b0aeaae8c540bb3e6dee36fd2",
                url: "https://cdn.sanity.io/images/xegcworx/test/b738ea7c5bb1fa7b0aeaae8c540bb3e6dee36fd2-412x286.jpg",
                _rev: "A48zhbJhtmHRCPZnQUSc3u",
                extension: "jpg",
                uploadId: "DxgtpC5em6r9dKiff1uHWkT4LpAYkMrz",
                mimeType: "image/jpeg",
                assetId: "b738ea7c5bb1fa7b0aeaae8c540bb3e6dee36fd2",
                _createdAt: "2023-11-21T14:18:24Z",
                originalFilename: "steg2.jpg",
                metadata: {
                  _type: "sanity.imageMetadata",
                  palette: {
                    dominant: {
                      background: "#bcbebf",
                      _type: "sanity.imagePaletteSwatch",
                      foreground: "#000",
                      title: "#fff",
                      population: 8.81,
                    },
                    _type: "sanity.imagePalette",
                    darkMuted: {
                      foreground: "#fff",
                      title: "#fff",
                      population: 7.05,
                      background: "#303948",
                      _type: "sanity.imagePaletteSwatch",
                    },
                    muted: {
                      background: "#548ca4",
                      _type: "sanity.imagePaletteSwatch",
                      foreground: "#fff",
                      title: "#fff",
                      population: 0,
                    },
                    lightVibrant: {
                      background: "#e4745c",
                      _type: "sanity.imagePaletteSwatch",
                      foreground: "#fff",
                      title: "#fff",
                      population: 0,
                    },
                    darkVibrant: {
                      foreground: "#fff",
                      title: "#fff",
                      population: 0,
                      background: "#7e1606",
                      _type: "sanity.imagePaletteSwatch",
                    },
                    lightMuted: {
                      foreground: "#000",
                      title: "#fff",
                      population: 8.81,
                      background: "#bcbebf",
                      _type: "sanity.imagePaletteSwatch",
                    },
                    vibrant: {
                      background: "#f22c0c",
                      _type: "sanity.imagePaletteSwatch",
                      foreground: "#fff",
                      title: "#fff",
                      population: 4.34,
                    },
                  },
                  hasAlpha: false,
                  lqip: "data:image/jpeg;base64,/9j/2wBDAAYEBQYFBAYGBQYHBwYIChAKCgkJChQODwwQFxQYGBcUFhYaHSUfGhsjHBYWICwgIyYnKSopGR8tMC0oMCUoKSj/2wBDAQcHBwoIChMKChMoGhYaKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCj/wAARCAAOABQDASIAAhEBAxEB/8QAFwABAQEBAAAAAAAAAAAAAAAAAAYCB//EACIQAAEEAgEEAwAAAAAAAAAAAAEAAgMRBAUGEhQhMSJhcf/EABYBAQEBAAAAAAAAAAAAAAAAAAMBAv/EABsRAAICAwEAAAAAAAAAAAAAAAECABEDEiGB/9oADAMBAAIRAxEAPwC4OowjG5zNZ3VmqDapbZh6pjGNfxcl3oldDh3uLHG1vZ+QPYpJeSYsbC44RNfih2J40VHxqKZL9MkJOOaj4kaiGOwD02ippeRYsjuoYZHj6RbsweT/2Q==",
                  dimensions: {
                    _type: "sanity.imageDimensions",
                    width: 412,
                    aspectRatio: 1.4405594405594406,
                    height: 286,
                  },
                  isOpaque: true,
                  blurHash: "VSOWgAR%JTofRk00NGbbj[kC5QoMrrogoL?^ofn4flV@",
                },
                _type: "sanity.imageAsset",
              },
            },
            {
              markDefs: [],
              children: [
                {
                  text: "Arbeidsprosessen legger seg på oppgavelisten din.",
                  _key: "9dff488d08350",
                  _type: "span",
                  marks: [],
                },
              ],
              _type: "block",
              style: "normal",
              _key: "dc9ef7eeb495",
            },
          ],
          _key: "7bb96aa9c6d0",
        },
        {
          navn: "Denne oppskriften gjelder for...",
          innhold: [
            {
              _key: "614bbf4cf3ba",
              listItem: "bullet",
              markDefs: [
                {
                  _type: "link",
                  href: "https://navno.sharepoint.com/sites/enhet-nav-ost-viken/SitePages/Arbeidsforberedende%20trening%20(AFT).aspx",
                  _key: "820ab5f022bb",
                },
                {
                  _type: "link",
                  href: "https://navno.sharepoint.com/sites/enhet-nav-ost-viken/SitePages/Arbeidsforberedende%20trening%20(AFT).aspx",
                  _key: "7ec35dc61916",
                },
                {
                  _type: "link",
                  href: "https://navno.sharepoint.com/sites/enhet-nav-ost-viken/SitePages/Arbeidsforberedende%20trening%20(AFT).aspx",
                  _key: "a0b98aed6a82",
                },
              ],
              children: [
                {
                  _key: "c9af855a65f90",
                  _type: "span",
                  marks: ["820ab5f022bb"],
                  text: "Arbeidsforberedende trening (",
                },
                {
                  _key: "c9af855a65f91",
                  _type: "span",
                  marks: ["7ec35dc61916"],
                  text: "AFT",
                },
                {
                  _type: "span",
                  marks: ["a0b98aed6a82"],
                  text: ")",
                  _key: "c9af855a65f92",
                },
              ],
              level: 1,
              _type: "block",
              style: "normal",
            },
            {
              children: [
                {
                  text: "Arbeidsrettet rehabilitering (ARR)",
                  _key: "72873e3ef4150",
                  _type: "span",
                  marks: ["484a28d4a960"],
                },
              ],
              level: 1,
              _type: "block",
              style: "normal",
              _key: "869380a3c489",
              listItem: "bullet",
              markDefs: [
                {
                  _type: "link",
                  href: "https://navno.sharepoint.com/sites/enhet-nav-ost-viken/SitePages/Arbeidsrettet-rehabilitering.aspx",
                  _key: "484a28d4a960",
                },
              ],
            },
            {
              listItem: "bullet",
              markDefs: [
                {
                  _type: "link",
                  href: "https://navno.sharepoint.com/sites/enhet-nav-ost-viken/SitePages/Avklaring,-Øst-Viken.aspx",
                  _key: "491bb5aeac10",
                },
              ],
              children: [
                {
                  _type: "span",
                  marks: ["491bb5aeac10"],
                  text: "Avklaring",
                  _key: "5c0c6fca03ec0",
                },
              ],
              level: 1,
              _type: "block",
              style: "normal",
              _key: "7e46eb50c58e",
            },
            {
              _key: "67405159ee89",
              listItem: "bullet",
              markDefs: [
                {
                  _type: "link",
                  href: "https://navno.sharepoint.com/sites/enhet-nav-ost-viken/SitePages/Oppfølging,-Øst-Viken.aspx",
                  _key: "af57700f94d6",
                },
              ],
              children: [
                {
                  text: "Oppfølging",
                  _key: "22896a6539b10",
                  _type: "span",
                  marks: ["af57700f94d6"],
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
              _key: "1865b0d22d4e",
              listItem: "bullet",
              markDefs: [
                {
                  _type: "link",
                  href: "https://navno.sharepoint.com/sites/enhet-nav-ost-viken/SitePages/Varig-tilrettelagt-arbeid.aspx",
                  _key: "f1d97f21f63c",
                },
                {
                  _key: "2d4d14420e87",
                  _type: "link",
                  href: "https://navno.sharepoint.com/sites/enhet-nav-ost-viken/SitePages/Varig-tilrettelagt-arbeid.aspx",
                },
                {
                  _type: "link",
                  href: "https://navno.sharepoint.com/sites/enhet-nav-ost-viken/SitePages/Varig-tilrettelagt-arbeid.aspx",
                  _key: "72d5478e5327",
                },
              ],
              children: [
                {
                  _type: "span",
                  marks: ["f1d97f21f63c"],
                  text: "Varig tilrettelagt arbeid i skjermet sektor (",
                  _key: "9f3d47ee4f4b0",
                },
                {
                  _key: "9f3d47ee4f4b1",
                  _type: "span",
                  marks: ["2d4d14420e87"],
                  text: "VTA",
                },
                {
                  _key: "9f3d47ee4f4b2",
                  _type: "span",
                  marks: ["72d5478e5327"],
                  text: ")",
                },
              ],
            },
            {
              style: "normal",
              _key: "41eed5502041",
              markDefs: [],
              children: [
                {
                  _key: "e46486308c7b0",
                  _type: "span",
                  marks: [],
                  text: "For disse tiltakene har NAV Tiltak gjort den første registreringsjobben. Din oppgave er å søke inn på tiltaket og legge inn en begrunnelse. Du kan finne mer Informasjon om de enkelte tiltakene på tiltakslenkene over.",
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
                  text: "",
                  _key: "af223ec522ae",
                },
              ],
              _type: "block",
              style: "normal",
              _key: "ed20fde0fab9",
            },
            {
              _type: "tips",
              innhold: [
                {
                  markDefs: [
                    {
                      href: "https://navno.sharepoint.com/sites/enhet-nav-ost-viken/Delte%20dokumenter/Tiltak/Tiltaksoversikten.pdf",
                      _key: "06c3095257ad",
                      _type: "link",
                    },
                  ],
                  children: [
                    {
                      _key: "bf22d1068e800",
                      _type: "span",
                      marks: [],
                      text: "Oversikt over tiltakene og tiltaksnummer (=løpenummer) finner du i ",
                    },
                    {
                      _type: "span",
                      marks: ["06c3095257ad"],
                      text: "Tiltaksoversikten.",
                      _key: "99537cc7d4c9",
                    },
                    {
                      _type: "span",
                      marks: [],
                      text: "\n",
                      _key: "07e2b4fddde7",
                    },
                  ],
                  _type: "block",
                  style: "normal",
                  _key: "9d3658ee357e",
                },
              ],
              _key: "166562b942c9",
            },
          ],
          _key: "c4c7d84a2a09",
          _type: "steg",
        },
        {
          navn: "Hvis du tilhører NAV Rogaland",
          innhold: [
            {
              style: "normal",
              _key: "b8dacb825edd",
              markDefs: [],
              children: [
                {
                  text: "",
                  _key: "49ee3805a09b",
                  _type: "span",
                  marks: [],
                },
              ],
              _type: "block",
            },
            {
              _type: "alertMessage",
              variant: ["warning"],
              innhold: [
                {
                  markDefs: [],
                  children: [
                    {
                      _type: "span",
                      marks: [],
                      text: "Hvis du tilhører NAV Rogaland så må du lese det viktige under her...",
                      _key: "1ad0b6b9ed60",
                    },
                  ],
                  _type: "block",
                  style: "normal",
                  _key: "60be61a60870",
                },
              ],
              _key: "c7f467aac9a9",
            },
            {
              children: [
                {
                  _type: "span",
                  marks: [],
                  text: "Vi i Rogaland gjør ting på en litt annen måte. Dere må blant annet sende sak til NAV Tiltak Rogaland etter registrering.",
                  _key: "47e1f88f2d09",
                },
              ],
              _type: "block",
              style: "normal",
              _key: "14ed2adf3879",
              markDefs: [],
            },
          ],
          _key: "8a100577eb22",
          _type: "steg",
        },
      ],
    },
  ],
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
    forHvemInfoboks: "For hvem infoboks med alert informasjon",
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
  innsatsgruppe: {
    sanityId: "8dcfe56e-0018-48dd-a9f5-817f6aec0b0d",
    beskrivelse: "Spesielt tilpasset innsats ",
    nokkel: Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
    tittel: VeilederflateInnsatsgruppe.tittel.SPESIELT_TILPASSET_INNSATS,
    order: 2,
  },
  delingMedBruker:
    "Hei <Fornavn>, \n\nVi har nå et arbeidsmarkedstiltak som jeg tenker kan passe deg godt. Det heter <tiltaksnavn> og er et tilbud for deg som deg som har nedsatt arbeidsevne og trenger hjelp for å komme i jobb.\n\nMålet er å avklare arbeidsevnen din gjennom arbeidstrening i ulike arbeidssituasjoner.\n\n- Etter en periode med forberedende arbeidstrening i et tilrettelagt arbeidsmiljø får du arbeidstrening i en vanlig bedrift.\n- Du får kartlagt kompetansen din og får karriereveiledning.\n- Du kan få tilrettelagt opplæring hvis du ønsker å gå videre med et yrkesfaglig utdanningsløp. Opplæringen skal bedre mulighetene dine til å komme i jobb.\n\nDu kan lese mer om kurset på www.nav.no/arbeidsforberedende-trening",
};
