import { BiBuildings } from "react-icons/bi";
import { Rule } from "@sanity/types";

export default {
  name: "arrangor",
  title: "Arrangør",
  type: "document",
  icon: BiBuildings,
  fields: [
    {
      name: "selskapsnavn",
      title: "Navn på selskap",
      type: "string",
      validation: (Rule: Rule) => Rule.required(),
    },
    {
      name: "telefonnummer",
      title: "Telefonnummer",
      type: "string",
      validation: (Rule: Rule) => Rule.required(),
    },
    {
      name: "epost",
      title: "E-post",
      type: "string",
    },
    {
      name: "adresse",
      title: "Adresse",
      type: "string",
      validation: (Rule: Rule) => Rule.required(),
    },
  ],
  preview: {
    select: {
      title: "selskapsnavn",
    },
  },
};
