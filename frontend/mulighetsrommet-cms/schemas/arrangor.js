export default {
  name: "tiltaksarrangor",
  title: "Tiltaksarrangør",
  type: "document",
  fields: [
    {
      name: "title",
      title: "Navn på kontaktperson",
      type: "string",
      validation: (Rule) => Rule.required(),
    },
    {
      name: "telefonnummer",
      title: "Telefonnummer",
      type: "number",
      validation: (Rule) => Rule.required(),
    },
    {
      name: "epost",
      title: "E-post",
      type: "string",
      validation: (Rule) => Rule.required(),
    },
    {
      name: "adresse",
      title: "Adresse",
      type: "string",
      validation: (Rule) => Rule.required(),
    },
  ],
  preview: {
    select: {
      title: "title",
    },
  },
};
