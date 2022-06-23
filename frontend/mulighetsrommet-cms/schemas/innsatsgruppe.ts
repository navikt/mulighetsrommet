import { Rule } from "@sanity/types";

export default {
  name: "innsatsgruppe",
  title: "Innsatsgruppe",
  type: "document",
  fields: [
    {
      name: "tittel",
      title: "Tittel",
      type: "string",
      validation: (Rule: Rule) => Rule.required(),
    },
    {
      name: "beskrivelse",
      title: "Beskrivelse",
      type: "string",
      validation: (Rule: Rule) => Rule.required(),
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
      const { title, order } = selection;
      return {
        title,
        subtitle: `Sorteringsrekkefølge = ${order}`,
      };
    },
  },
};
