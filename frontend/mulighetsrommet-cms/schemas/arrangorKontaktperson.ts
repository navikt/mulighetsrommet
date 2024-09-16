import { defineField } from "sanity";

export const arrangorKontaktperson = {
  title: "Arrangør kontaktperson",
  name: "arrangorKontaktperson",
  type: "document",
  fields: [
    defineField({
      title: "Navn",
      name: "navn",
      type: "string",
      validation: (rule) => rule.required().min(2).max(200),
    }),
    defineField({
      title: "Telefon",
      name: "telefon",
      type: "string",
      validation: (rule) => rule.min(2).max(200),
    }),
    defineField({
      title: "E-post",
      name: "epost",
      type: "string",
      validation: (rule) => rule.required().email(),
    }),
  ],
  preview: {
    select: {
      title: "navn",
      subtitle: "epost",
    },
  },
};
