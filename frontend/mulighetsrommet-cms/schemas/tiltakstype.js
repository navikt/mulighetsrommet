import { FaWpforms } from "react-icons/fa";

export default {
  name: "tiltakstype",
  title: "Tiltakstype",
  type: "document",
  icon: FaWpforms,
  fields: [
    {
      name: "tiltakstypeNavn",
      title: "Navn på tiltakstype",
      type: "string",
      validation: (Rule) => Rule.required().min(2).max(200),
    },
    {
      name: "beskrivelse",
      title: "Beskrivelse",
      type: "string",
    },
    {
      name: "overgangTilArbeid",
      title: "Overgang til arbeid",
      description: "Hentes fra Arena, usikker på hvordan denne skal vises her",
      type: "blockContent",
    },
    {
      name: "innsatsgruppe",
      title: "Innsatsgruppe",
      type: "reference",
      options: {
        disableNew: true,
      },
      to: [{ type: "innsatsgruppe" }],
    },
    {
      name: "varighet",
      title: "Varighet",
      type: "string",
    },
    //TODO skal kunne legge til flere lenker og filer
    {
      name: "regelverkFil",
      title: "Regelverk fil",
      type: "file",
    },
    {
      name: "regelverkFilNavn",
      title: "Navn til fil",
      description: "Hvilket navn skal vises til filen?",
      type: "string",
      validation: (Rule) =>
        Rule.custom((field, context) =>
          context.document.regelverkFil && field === undefined
            ? "Dette feltet kan ikke være tomt."
            : true
        ),
      hidden: ({ document }) => !document?.regelverkFil,
    },
    {
      name: "regelverkLenke",
      title: "Regelverk lenke",
      type: "url",
      placeholder: "https://www...",
    },
    {
      name: "regelverkLenkeNavn",
      title: "Navn til lenke",
      description: "Hvilket navn skal vises i lenken?",
      type: "string",
      validation: (Rule) =>
        Rule.custom((field, context) =>
          context.document.regelverkLenke && field === undefined
            ? "Dette feltet kan ikke være tomt."
            : true
        ),
      hidden: ({ document }) => !document?.regelverkLenke,
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
        {
          name: "innsikt",
          title: "Innsikt",
          description:
            "Hentes fra Arena, usikker på hvordan denne skal vises her",
          type: "blockContent",
        },
      ],
    },
  ],
  preview: {
    select: {
      title: "tiltakstypeNavn",
      subtitle: "innsatsgruppe.tittel",
    },
  },
};
