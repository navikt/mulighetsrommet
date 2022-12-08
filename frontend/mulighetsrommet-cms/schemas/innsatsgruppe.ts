import { defineType, defineField } from "sanity";

export const innsatsgruppe = defineType({
  name: "innsatsgruppe",
  title: "Innsatsgruppe",
  type: "document",
  fields: [
    defineField({
      name: "tittel",
      title: "Tittel",
      type: "string",
      validation: (Rule) => Rule.required(),
    }),
    defineField({
      name: "beskrivelse",
      title: "Beskrivelse",
      type: "string",
      validation: (Rule) => Rule.required(),
    }),
    defineField({
      name: "order",
      title: "Rekkefølge",
      type: "number",
    }),
    defineField({
      name: "nokkel",
      title: "Nøkkel",
      type: "string",
      validation: (Rule) => Rule.required(),
    }),
  ],
  preview: {
    select: {
      title: "tittel",
      order: "order",
    },
  },
});
