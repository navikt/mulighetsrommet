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
      validation: (Rule) => Rule.required().min(2).max(200),
    }),
    defineField({
      name: "redaktor",
      title: "Administratorer",
      type: "array",
      description: "Eiere av innholdet i denne tiltakstypen.",
      of: [
        {
          type: "reference",
          to: [{ type: "redaktor" }],
        },
      ],
      validation: (rule) => rule.required().unique(),
    }),
     defineField({
      name: "beskrivelse",
      title: "Beskrivelse",
      type: "text",
      rows: 5,
      validation: (Rule) => Rule.max(1500),
      description: "Kort beskrivelse av formål med tiltaket. ",
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
      description: "Innsatsgrupper som kan delta på tiltaket.",
      type: "reference",
      options: {
        disableNew: true,
      },
      to: [{ type: "innsatsgruppe" }],
    }),
    defineField({
      name: "regelverkLenker",
      title: "Regelverk",
      type: "array",
      of: [{ type: "reference", to: [{ type: "regelverklenke" }] }],
    }),
    defineField({
      name: "faneinnhold",
      title: "Faneinnhold",
      type: "faneinnhold",
    }),

    defineField({
      name: "delingMedBruker",
      title: "Informasjon som kan deles med bruker",
      description: "Informasjon om tiltaket som veileder kan dele med bruker.",
      type: "text",
    }),

    defineField({
      name: "forskningsrapport",
      title: "Forskningsrapport",
      description:
        "Legg til en eller flere forskningsrapporter som gjelder for tiltakstypen. Disse vil bli vist under 'Innsikt'-fanen.",
      type: "array",
      of: [{ type: "reference", to: [{ type: "forskningsrapport" }] }],
      hidden: true, //Skjules frem til innsiktsfanen er klar
    }),
  ],
  preview: {
    select: {
      title: "tiltakstypeNavn",
      subtitle: "innsatsgruppe.tittel",
    },
  },
});
