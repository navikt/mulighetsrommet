import { defineField } from "sanity";

export const arrangor = {
  title: "Arrangør",
  name: "arrangor",
  type: "document",
  fields: [
    defineField({
      title: "Navn på tiltaksarrangør",
      name: "navn",
      type: "string",
      validation: (Rule) => Rule.min(3).required(),
    }),
    defineField({
      title: "Organisasjonsnummer",
      name: "organisasjonsnummer",
      type: "string",
      validation: Rule => Rule.regex(/^\\d{9}$/).error("Organisasjonsnummer må bestå av 9 siffer")
    }),
    defineField({
      title: "Kontaktpersoner",
      name: "kontaktpersoner",
      type: "array",
      of: [
        {
          type: "reference",
          to: [{ type: "arrangorKontaktperson" }],
        },
      ],
    }),
  ],
  preview: {
    select: {
      title: "navn",
    },
  },
};
