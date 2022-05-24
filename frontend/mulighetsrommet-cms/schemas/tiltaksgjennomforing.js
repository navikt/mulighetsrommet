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
      name: "title",
      title: "Navn på tiltaksgjennomføring",
      type: "string",
      validation: (Rule) => Rule.required(),
    },
    {
      name: "tiltaksnummer",
      title: "Tiltaksnummer",
      type: "number",
    },
    {
      name: "leverandor",
      title: "Leverandør",
      type: "string",
    },
    {
      name: "lokasjon",
      title: "Lokasjon",
      type: "string",
    },
    {
      name: "enheter",
      title: "Enheter",
      description:
        "Hvilke enheter skal ha tilgang til denne tiltaksgjennomføringen?",
      type: "document",
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
              { title: "Vest-Viken", value: "vestviken" },
              { title: "Øst-Viken", value: "ostviken" },
            ],
          },
        },
        //innlandet
        {
          name: "ringsaker",
          title: "Ringsaker",
          type: "boolean",
          initialValue: false,
          hidden: ({ parent }) =>
            parent?.fylke === "trondelag" ||
            parent?.fylke === "vestviken" ||
            parent?.fylke === "ostviken" ||
            parent?.fylke === undefined,
        },
        //trøndelag
        {
          name: "trondheim",
          title: "Trondheim",
          type: "boolean",
          initialValue: false,
          hidden: ({ parent }) =>
            parent?.fylke === "innlandet" ||
            parent?.fylke === "vestviken" ||
            parent?.fylke === "ostviken" ||
            parent?.fylke === undefined,
        },
        {
          name: "steinkjer",
          title: "Steinkjer",
          type: "boolean",
          initialValue: false,
          hidden: ({ parent }) =>
            parent?.fylke === "innlandet" ||
            parent?.fylke === "vestviken" ||
            parent?.fylke === "ostviken" ||
            parent?.fylke === undefined,
        },
        //vest-viken
        {
          name: "asker",
          title: "Asker",
          type: "boolean",
          initialValue: false,
          hidden: ({ parent }) =>
            parent?.fylke === "innlandet" ||
            parent?.fylke === "trondelag" ||
            parent?.fylke === "ostviken" ||
            parent?.fylke === undefined,
        },
        //øst-viken
        {
          name: "lillestrom",
          title: "Lillestrøm",
          type: "boolean",
          initialValue: false,
          hidden: ({ parent }) =>
            parent?.fylke === "innlandet" ||
            parent?.fylke === "trondelag" ||
            parent?.fylke === "vestviken" ||
            parent?.fylke === undefined,
        },
        {
          name: "sarpsborg",
          title: "Sarpsborg",
          type: "boolean",
          initialValue: false,
          hidden: ({ parent }) =>
            parent?.fylke === "innlandet" ||
            parent?.fylke === "trondelag" ||
            parent?.fylke === "vestviken" ||
            parent?.fylke === undefined,
        },
        {
          name: "fredrikstad",
          title: "Fredrikstad",
          type: "boolean",
          initialValue: false,
          hidden: ({ parent }) =>
            parent?.fylke === "innlandet" ||
            parent?.fylke === "trondelag" ||
            parent?.fylke === "vestviken" ||
            parent?.fylke === undefined,
        },
        {
          name: "indreostfold",
          title: "Indre Østfold",
          type: "boolean",
          initialValue: false,
          hidden: ({ parent }) =>
            parent?.fylke === "innlandet" ||
            parent?.fylke === "trondelag" ||
            parent?.fylke === "vestviken" ||
            parent?.fylke === undefined,
        },
        {
          name: "skiptvedtmarker",
          title: "Skiptvedt/Marker",
          type: "boolean",
          initialValue: false,
          hidden: ({ parent }) =>
            parent?.fylke === "innlandet" ||
            parent?.fylke === "trondelag" ||
            parent?.fylke === "vestviken" ||
            parent?.fylke === undefined,
        },
      ],
    },
    {
      //TODO denne skal være hidden om "løpende" oppstart på valgt på tiltakstype
      name: "oppstartsdato",
      title: "Oppstart dato",
      type: "date",
      options: { dateFormat: "DD/MM/YYYY" },
      hidden: async ({ parent }) => {
        const ref = parent.tiltakstype._ref;
        const result = await groq(`*[_type == "tiltakstype" && _id == ${ref}]`);
        return result.oppstart !== "dato";
      },
    },
    //Faneinnhold
    {
      name: "faneinnhold",
      title: "Innhold faner",
      type: "document",
      fields: [
        {
          name: "forhvem",
          title: "For hvem",
          type: "blockContent",
        },
        {
          name: "detaljeroginnhold",
          title: "Detaljer og innhold",
          type: "blockContent",
        },
        {
          name: "pameldingogvarighet",
          title: "Påmelding og varighet",
          type: "blockContent",
        },
        {
          name: "kontaktinfo",
          title: "Kontaktinfo",
          type: "document",
          fields: [
            {
              name: "kontaktinfoleverandor",
              title: "Leverandør",
              type: "reference",
              to: [{ type: "tiltaksarrangor" }],
              validation: (Rule) => Rule.required(),
            },
            {
              name: "kontaktinfotiltaksansvarlig",
              title: "Tiltaksansvarlig",
              type: "reference",
              to: [{ type: "navkontaktperson" }],
              validation: (Rule) => Rule.required(),
            },
          ],
        },
      ],
    },
  ],
  preview: {
    select: {
      title: "title",
    },
  },
};
