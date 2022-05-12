export default {
  name: "tiltakstype",
  title: "Tiltakstype",
  type: "document",
  fields: [
    {
      name: "title",
      title: "Navn på tiltakstype",
      type: "string",
      validation: (Rule) => Rule.required().min(2).max(200),
    },
    {
      name: "ingress",
      title: "Ingress",
      type: "blockContent",
      validation: (Rule) => Rule.required(),
    },
    //  Sammendrag/Infoboks
    {
      name: "innsatsgruppe",
      title: "Innsatsgruppe",
      type: "string",
      options: {
        layout: "dropdown",
        list: [
          { title: "Standardinnsats", value: "staninn" },
          { title: "Situasjonsbestemt innsats", value: "sitinn" },
          { title: "Spesielt tilpasset innsats", value: "speinn" },
          { title: "Varig tilpasset innsats", value: "varinn" },
        ],
      },
      validation: (Rule) => Rule.required(),
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
      name: "beskrivelse",
      title: "Beskrivelse/Kravspek/Lovdata",
      type: "file",
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
          name: "kontaktinfofagansvarlig",
          title: "Kontaktinfo fagansvarlig",
          type: "reference",
          to: [{ type: "navkontaktperson" }],
          validation: (Rule) => Rule.required(),
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
