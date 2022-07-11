import { defineType } from "sanity";

export default defineType({
  name: "innsatsgruppe",
  title: "Innsatsgruppe",
  type: "document",
  fields: [
    {
      name: "tittel",
      title: "Tittel",
      type: "string",
      validation: (Rule) => Rule.required(),
    },
    {
      name: "beskrivelse",
      title: "Beskrivelse",
      type: "string",
      validation: (Rule) => Rule.required(),
    },
    {
      name: "order",
      title: "Rekkefølge",
      type: "number",
    },
  ],
  preview: {
    select: {
      title: "tittel",
      order: "order",
    },
    prepare: (selection) => {
      const { title, order } = selection as Record<string, string>;
      return {
        title,
        subtitle: `Sorteringsrekkefølge = ${order}`,
      };
    },
  },
});
