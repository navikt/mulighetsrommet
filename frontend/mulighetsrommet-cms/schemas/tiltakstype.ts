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
      description: "Her legger du inn navn for tiltakstypen.",
      validation: (Rule) => Rule.required().min(2).max(200),
    },
    {
      name: "beskrivelse",
      title: "Beskrivelse",
      type: "text",
      rows: 5,
      validation: (Rule) => Rule.max(500),
      description:
        "Her kan du legge til en tekstlig beskrivelse av tiltakstypen.",
    },
    {
      name: "tiltaksgruppe",
      title: "Individuelt- eller gruppetiltak?",
      description:
        "Her velger du om tiltaket er et individuelt- eller et gruppetiltak.",
      type: "string",
      options: {
        list: [
          { title: "Individuelt tiltak", value: "individuelt" },
          { title: "Gruppetiltak", value: "gruppe" },
        ],
      },
      layout: "radio",
      validation: (Rule) =>
        Rule.required().error("Du må velge ett av alternativene."),
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
      description:
        "Her velger du hvilken innsatsgruppe tiltakstypen er ment for.",
      type: "reference",
      options: {
        disableNew: true,
      },
      to: [{ type: "innsatsgruppe" }],
    },
    {
      name: "varighet",
      title: "Varighet",
      description: "Her kan du legge til hvor lang varighet tiltakstypen har.",
      type: "string",
    },
    {
      name: "regelverkLenker",
      title: "Regelverkslenker",
      type: "array",
      of: [{ type: "reference", to: [{ type: "regelverklenke" }] }],
    },
    {
      name: "faneinnhold",
      title: "Faneinnhold",
      type: "faneinnhold",
    },
    {
      name: "forskningsrapport",
      title: "Forskningsrapport",
      description:
        "Legg til en eller flere forskningsrapporter som gjelder for tiltakstypen. Disse vil bli vist under 'Innsikt'-fanen.",
      type: "array",
      of: [{ type: "reference", to: [{ type: "forskningsrapport" }] }],
    },
    {
      name: "delingMedBruker",
      title: "Informasjon til å dele med bruker",
      description:
        "Dette er teksten som veileder kan dele med bruker via 'Del med bruker'-knapp.",
      type: "text",
    },
  ],
  preview: {
    select: {
      title: "tiltakstypeNavn",
      subtitle: "innsatsgruppe.tittel",
    },
  },
};
