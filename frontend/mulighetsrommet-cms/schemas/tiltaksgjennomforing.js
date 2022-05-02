export default {
  name: "tiltaksgjennomforing",
  title: "Tiltaksgjennomføring",
  type: "document",
  fields: [
    {
      name: "tiltaksgjennomforingtiltakstype",
      title: "Tiltakstype",
      type: "reference",
      to: [{ type: "tiltakstype" }],
      validation: (Rule) => Rule.required(),
    },
    {
      name: "title",
      title: "Tittel",
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
      ],
    },
    {
      name: "kontaktinfo",
      title: "Kontaktinfo",
      type: "document",
      fields: [
        {
          name: "kontaktinfoleverandor",
          title: "Leverandør",
          type: "document",
          fields: [
            {
              name: "navnkontaktperson",
              title: "Navn på kontaktperson",
              type: "string",
              validation: (Rule) => Rule.required().min(2).max(200),
            },
            {
              name: "telefonnummer",
              title: "Telefonnummer",
              type: "string",
              validation: (Rule) => Rule.required().min(2).max(200),
            },
            {
              name: "epost",
              title: "E-post",
              type: "string",
              validation: (Rule) => Rule.required().min(2).max(200),
            },
            {
              name: "adresse",
              title: "Adresse",
              type: "string",
              validation: (Rule) => Rule.required().min(2).max(200),
            },
          ],
        },
        {
          name: "kontaktinfotiltaksansvarlig",
          title: "Tiltaksansvarlig",
          type: "document",
          fields: [
            {
              name: "tiltaksansvarlig",
              title: "Navn",
              type: "string",
              validation: (Rule) => Rule.required().min(2).max(200),
            },
            {
              name: "telefonnummer",
              title: "Telefonnummer",
              type: "string",
              validation: (Rule) => Rule.required().min(2).max(200),
            },
            {
              name: "epost",
              title: "E-post",
              type: "string",
              validation: (Rule) => Rule.required().min(2).max(200),
            },
            {
              name: "navkontor",
              title: "NAV-kontor",
              type: "string",
              validation: (Rule) => Rule.required().min(2).max(200),
            },
          ],
        },
      ],
    },
  ],
  preview: {
    select: {
      title: "title",
      media: "promoImage",
    },
  },
};
