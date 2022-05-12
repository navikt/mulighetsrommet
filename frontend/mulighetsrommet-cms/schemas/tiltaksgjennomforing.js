export default {
  name: "tiltaksgjennomforing",
  title: "Tiltaksgjennomføring",
  type: "document",
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
    //Kan hende denne er unødvendig
    {
      name: "slug",
      type: "slug",
      description: "Url til tiltaksgjennomforing",
      options: {
        source: "title",
        maxLength: 40,
      },
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
