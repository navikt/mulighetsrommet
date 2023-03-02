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
      validation: (Rule) => Rule.max(1500),
      description:
        "Her kan du legge til en tekstlig beskrivelse av tiltakstypen.",
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
    // defineField({
    //   name: "forskningsrapport",
    //   title: "Forskningsrapport",
    //   description:
    //     "Legg til en eller flere forskningsrapporter som gjelder for tiltakstypen. Disse vil bli vist under 'Innsikt'-fanen.",
    //   type: "array",
    //   of: [{ type: "reference", to: [{ type: "forskningsrapport" }] }],
    // }),
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
