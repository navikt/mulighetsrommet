import { FaBook } from "react-icons/fa";
import { defineType, defineField } from "sanity";

export const forskningsrapport = defineType({
  name: "forskningsrapport",
  title: "Forskningsrapport",
  type: "document",
  icon: FaBook,
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
      type: "blockContent",
      validation: (Rule) => Rule.required(),
    }),
    defineField({
      name: "lenker",
      title: "Lenker",
      description: "Her legger du til lenker til forskningen.",
      type: "array",
      of: [{ type: "lenke" }],
    }),
  ],
  preview: {
    select: {
      title: "tittel",
    },
  },
});
