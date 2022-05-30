import { FaWpforms } from "react-icons/fa";

export default {
  name: "tiltakstype",
  title: "Tiltakstype",
  type: "document",
  icon: FaWpforms,
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
          name: "kontaktinfoFagansvarlig",
          title: "Kontaktinfo fagansvarlig",
          type: "reference",
          to: [{ type: "navKontaktperson" }],
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
