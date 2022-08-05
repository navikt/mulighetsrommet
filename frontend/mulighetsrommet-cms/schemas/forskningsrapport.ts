import { Rule } from "@sanity/types";
import { FaBook } from "react-icons/fa";

export default {
  name: "forskningsrapport",
  title: "Forskningsrapport",
  type: "document",
  icon: FaBook,
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
      type: "blockContent",
      validation: (Rule: Rule) => Rule.required(),
    },
    {
      name: "lenker",
      title: "Lenker",
      description: "Legg til lenker til forskningen",
      type: "array",
      of: [{ type: "lenke" }],
    },
  ],
  preview: {
    select: {
      title: "tittel",
    },
  },
};
