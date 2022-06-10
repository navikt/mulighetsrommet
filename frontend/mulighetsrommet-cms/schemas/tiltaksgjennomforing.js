import { GrDocumentPerformance } from "react-icons/gr";

export default {
  name: "tiltaksgjennomforing",
  title: "Tiltaksgjennomføring",
  type: "document",
  icon: GrDocumentPerformance,
  fields: [
    {
      name: "tiltakstype",
      title: "Tiltakstype",
      type: "reference",
      to: [{ type: "tiltakstype" }],
      validation: (Rule) => Rule.required(),
    },
    {
      name: "tiltaksgjennomforingNavn",
      title: "Navn på tiltaksgjennomføring",
      type: "string",
      validation: (Rule) => Rule.required(),
    },
    {
      name: "beskrivelse",
      title: "Beskrivelse",
      type: "string",
    },
    {
      name: "tiltaksnummer",
      title: "Tiltaksnummer",
      type: "number",
      validation: (Rule) => Rule.required(),
    },
    {
      name: "kontaktinfoArrangor",
      title: "Arrangør",
      type: "reference",
      to: [{ type: "arrangor" }],
      validation: (Rule) => Rule.required(),
    },
    {
      name: "lokasjon",
      title: "Lokasjon",
      type: "string",
      validation: (Rule) => Rule.required(),
    },
    {
      name: "enheter",
      title: "Enheter",
      description:
        "Hvilke enheter skal ha tilgang til denne tiltaksgjennomføringen?",
      type: "object",
      fields: [
        {
          name: "fylke",
          title: "Fylke",
          type: "string",
          options: {
            layout: "dropdown",
            list: [
              { title: "Innlandet", value: "innlandet" },
              { title: "Trøndelag", value: "trondelag" },
              { title: "Vest-Viken", value: "vestViken" },
              { title: "Øst-Viken", value: "ostViken" },
            ],
          },
        },
        //innlandet
        {
          name: "ringsaker",
          title: "Ringsaker",
          type: "boolean",
          initialValue: false,
          hidden: ({ parent }) => parent?.fylke !== "innlandet",
        },
        //trøndelag
        {
          name: "trondheim",
          title: "Trondheim",
          type: "boolean",
          initialValue: false,
          hidden: ({ parent }) => parent?.fylke !== "trondelag",
        },
        {
          name: "steinkjer",
          title: "Steinkjer",
          type: "boolean",
          initialValue: false,
          hidden: ({ parent }) => parent?.fylke !== "trondelag",
        },
        //vest-viken
        {
          name: "asker",
          title: "Asker",
          type: "boolean",
          initialValue: false,
          hidden: ({ parent }) => parent?.fylke !== "vestViken",
        },
        //øst-viken
        {
          name: "lillestrom",
          title: "Lillestrøm",
          type: "boolean",
          initialValue: false,
          hidden: ({ parent }) => parent?.fylke !== "ostViken",
        },
        {
          name: "sarpsborg",
          title: "Sarpsborg",
          type: "boolean",
          initialValue: false,
          hidden: ({ parent }) => parent?.fylke !== "ostViken",
        },
        {
          name: "fredrikstad",
          title: "Fredrikstad",
          type: "boolean",
          initialValue: false,
          hidden: ({ parent }) => parent?.fylke !== "ostViken",
        },
        {
          name: "indreOstfold",
          title: "Indre Østfold",
          type: "boolean",
          initialValue: false,
          hidden: ({ parent }) => parent?.fylke !== "ostViken",
        },
        {
          name: "skiptvedtMarker",
          title: "Skiptvedt/Marker",
          type: "boolean",
          initialValue: false,
          hidden: ({ parent }) => parent?.fylke !== "ostViken",
        },
      ],
    },
    {
      name: "oppstart",
      title: "Oppstart",
      type: "string",
      options: {
        list: [
          { title: "Dato", value: "dato" },
          { title: "Løpende", value: "lopende" },
        ],
      },
      validation: (Rule) => Rule.required(),
    },
    {
      name: "oppstartsdato",
      title: "Oppstart dato",
      type: "date",
      options: { dateFormat: "DD/MM/YYYY" },
      hidden: ({ parent }) => parent?.oppstart !== "dato",
    },
    //Faneinnhold
    {
      name: "faneinnhold",
      title: "Innhold faner",
      type: "object",
      fields: [
        {
          name: "forHvemInfoboks",
          title: "For hvem - infoboks",
          description:
            "Hvis denne har innhold, vises det i en infoboks i fanen 'For hvem'",
          type: "string",
        },
        {
          name: "forHvem",
          title: "For hvem",
          type: "blockContent",
        },

        {
          name: "detaljerOgInnholdInfoboks",
          title: "Detaljer og innhold - infoboks",
          description:
            "Hvis denne har innhold, vises det i en infoboks i fanen 'Detaljer og innhold'",
          type: "string",
        },
        {
          name: "detaljerOgInnhold",
          title: "Detaljer og innhold",
          type: "blockContent",
        },

        {
          name: "pameldingOgVarighetInfoboks",
          title: "Påmelding og varighet - infoboks",
          description:
            "Hvis denne har innhold, vises det i en infoboks i fanen 'Påmelding og varighet'",
          type: "string",
        },
        {
          name: "pameldingOgVarighet",
          title: "Påmelding og varighet",
          type: "blockContent",
        },
      ],
    },
    //TODO skal kunne legge til flere tiltaksansvarlige
    {
      name: "kontaktinfoTiltaksansvarlig",
      title: "Tiltaksansvarlig",
      type: "reference",
      to: [{ type: "navKontaktperson" }],
      validation: (Rule) => Rule.required(),
    },
  ],
  preview: {
    select: {
      title: "tiltaksgjennomforingNavn",
    },
  },
};
