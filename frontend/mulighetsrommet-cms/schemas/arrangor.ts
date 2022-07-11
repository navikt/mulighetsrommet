import { BiBuildings } from "react-icons/bi";
import { defineType } from "sanity";

export default defineType({
  name: "arrangor",
  title: "Arrangør",
  type: "document",
  icon: BiBuildings,
  fields: [
    {
      name: "selskapsnavn",
      title: "Navn på selskap",
      type: "string",
      validation: (Rule) => Rule.required(),
    },
    {
      name: "telefonnummer",
      title: "Telefonnummer",
      type: "string",
      validation: (Rule) => Rule.required(),
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
      validation: (Rule) => Rule.required(),
    },
  ],
  preview: {
    select: {
      title: "selskapsnavn",
    },
  },
});
