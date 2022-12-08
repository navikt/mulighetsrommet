import { defineField } from "sanity";

export const lenke = {
  name: "lenke",
  title: "Lenke",
  type: "object",
  fields: [
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
