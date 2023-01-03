import { defineField } from "sanity";
import { Information } from "../components/Information";

export const lenke = {
  name: "lenke",
  title: "Lenke",
  type: "object",
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
      title: "Lenke",
      name: "lenke",
      type: "string",
    }),
    defineField({
      title: "Lenkenavn",
      name: "lenkenavn",
      type: "string",
    }),
  ],
  preview: {
    select: {
      title: "lenkenavn",
      subtitle: "lenke",
    },
  },
};
