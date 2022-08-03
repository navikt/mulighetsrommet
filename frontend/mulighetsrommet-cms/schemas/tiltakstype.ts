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
      name: "nokkelinfoKomponenter",
      title: "Nøkkelinfo",
      type: "array",
      of: [{ type: "nokkelinfo" }],
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
    {
      name: "regelverkFiler",
      title: "Regelverk filer",
      type: "array",
      of: [{ type: "reference", to: [{ type: "regelverkfil" }] }],
    },
    {
      name: "regelverkLenker",
      title: "Regelverk lenker",
      type: "array",
      of: [{ type: "reference", to: [{ type: "regelverklenke" }] }],
    },
    {
      name: "faneinnhold",
      title: "Faneinnhold",
      type: "faneinnhold",
    },
  ],
  preview: {
    select: {
      title: "tiltakstypeNavn",
      subtitle: "innsatsgruppe.tittel",
    },
  },
};
