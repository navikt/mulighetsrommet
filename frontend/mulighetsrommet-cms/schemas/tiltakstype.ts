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
      readOnly: true,
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
      name: "kombinasjon",
      title: "Kan kombineres med",
      description: "Her kan man legge til tiltakstyper som kan kombineres med denne tiltakstypen.",
      type: "array",
      of: [
        {
          type: "reference",
          to: [{ type: "tiltakstype" }],
        },
      ],
      validation: (Rule) => Rule.unique().error("Du kan bare ha én av hver tiltakstype"),
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
      name: "innsatsgrupper",
      title: "Innsatsgrupper",
      type: "array",
      readOnly: true,
      options: {
        layout: "tags",
      },
      of: [
        {
          type: "string",
          options: {
            list: [
              { value: "STANDARD_INNSATS" },
              { value: "SITUASJONSBESTEMT_INNSATS" },
              { value: "SPESIELT_TILPASSET_INNSATS" },
              { value: "GRADERT_VARIG_TILPASSET_INNSATS" },
              { value: "VARIG_TILPASSET_INNSATS" },
            ],
          },
        },
      ],
      validation: (Rule) => Rule.unique(),
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
      name: "oppskrifter",
      title: "Oppskrifter",
      description:
        "Her kan man velge å koble oppskrifter til tiltakstypen slik at de blir tilgjengelig for veiledere i Modia",
      type: "array",
      of: [{ type: "reference", to: [{ type: "oppskrift" }] }],
      validation: (Rule) =>
        Rule.length(1).error("Det kan kun være én oppskrift tilknyttet tiltakstypen"),
    }),
  ],
  preview: {
    select: {
      title: "tiltakstypeNavn",
      subtitle: "innsatsgruppe.tittel",
    },
  },
});
