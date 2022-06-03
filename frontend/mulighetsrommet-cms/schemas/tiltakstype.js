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
      type: "blockContent",
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
      type: "string",
      options: {
        layout: "dropdown",
        list: [
          { title: "Standardinnsats", value: "Standardinnsats" },
          {
            title: "Situasjonsbestemt innsats",
            value: "Situasjonsbestemt innsats",
          },
          {
            title: "Spesielt tilpasset innsats",
            value: "Spesielt tilpasset innsats",
          },
          {
            title: "Varig tilpasset innsats",
            value: "Varig tilpasset innsats",
          },
        ],
      },
      validation: (Rule) => Rule.required(),
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
      name: "regelverkLenke",
      title: "Regelverk lenke",
      type: "url",
    },
    //Faneinnhold
    {
      name: "faneinnhold",
      title: "Innhold faner",
      type: "document",
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
    },
  },
};
