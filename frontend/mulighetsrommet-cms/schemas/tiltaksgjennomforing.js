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
          name: "forHvem",
          title: "For hvem",
          type: "blockContent",
        },
        {
          name: "detaljerOgInnhold",
          title: "Detaljer og innhold",
          type: "blockContent",
        },
        {
          name: "pameldingOgVarighet",
          title: "Påmelding og varighet",
          type: "blockContent",
        },
        {
          name: "kontaktinfo",
          title: "Kontaktinfo",
          type: "document",
          fields: [
            {
              name: "kontaktinfoLeverandor",
              title: "Leverandør",
              type: "reference",
              to: [{ type: "leverandor" }],
              validation: (Rule) => Rule.required(),
            },
            {
              name: "kontaktinfoTiltaksansvarlig",
              title: "Tiltaksansvarlig",
              type: "reference",
              to: [{ type: "navKontaktperson" }],
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
