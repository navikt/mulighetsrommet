import { FaWpforms } from "react-icons/fa";
import { defineField, defineType } from "sanity";
import { Information } from "../components/Information";

export const tiltakstype = defineType({
  name: "tiltakstype",
  title: "Tiltakstype",
  type: "document",
  icon: FaWpforms,
  fields: [
    defineField({
      name: "info",
      title: "Info",
      type: "string",
      components: {
        field: Information,
      },
    }),
    defineField({
      name: "tiltakstypeNavn",
      title: "Navn på tiltakstype",
      type: "string",
      description: "Her legger du inn navn for tiltakstypen.",
      validation: (Rule) => Rule.required().min(2).max(200),
    }),
    defineField({
      name: "beskrivelse",
      title: "Beskrivelse",
      type: "text",
      rows: 5,
      validation: (Rule) => Rule.max(500),
      description:
        "Her kan du legge til en tekstlig beskrivelse av tiltakstypen.",
    }),
    defineField({
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
        layout: "radio",
      },
      validation: (Rule) =>
        Rule.required().error("Du må velge ett av alternativene."),
    }),
    defineField({
      name: "nokkelinfoKomponenter",
      title: "Nøkkelinfo",
      type: "array",
      of: [{ type: "nokkelinfo" }],
    }),
    defineField({
      name: "innsatsgruppe",
      title: "Innsatsgruppe",
      description:
        "Her velger du hvilken innsatsgruppe tiltakstypen er ment for.",
      type: "reference",
      options: {
        disableNew: true,
      },
      to: [{ type: "innsatsgruppe" }],
    }),
    defineField({
      name: "varighet",
      title: "Varighet",
      description: "Her kan du legge til hvor lang varighet tiltakstypen har.",
      type: "string",
    }),
    defineField({
      name: "regelverkLenker",
      title: "Regelverkslenker",
      type: "array",
      of: [{ type: "reference", to: [{ type: "regelverklenke" }] }],
    }),
    defineField({
      name: "faneinnhold",
      title: "Faneinnhold",
      type: "faneinnhold",
    }),
    defineField({
      name: "forskningsrapport",
      title: "Forskningsrapport",
      description:
        "Legg til en eller flere forskningsrapporter som gjelder for tiltakstypen. Disse vil bli vist under 'Innsikt'-fanen.",
      type: "array",
      of: [{ type: "reference", to: [{ type: "forskningsrapport" }] }],
    }),
    defineField({
      name: "delingMedBruker",
      title: "Informasjon til å dele med bruker",
      description:
        "Dette er teksten som veileder kan dele med bruker via 'Del med bruker'-knapp.",
      type: "text",
    }),
  ],
  preview: {
    select: {
      title: "tiltakstypeNavn",
      subtitle: "innsatsgruppe.tittel",
    },
  },
});
