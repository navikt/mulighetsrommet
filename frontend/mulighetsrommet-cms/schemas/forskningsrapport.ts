import { FaBook } from "react-icons/fa";
import { defineField, defineType } from "sanity";
import { Information } from "../components/Information";

export const forskningsrapport = defineType({
  name: "forskningsrapport",
  title: "Forskningsrapport",
  type: "document",
  icon: FaBook,
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
