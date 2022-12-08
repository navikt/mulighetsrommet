import { BiBuildings } from "react-icons/bi";
import { Rule } from "@sanity/types";
import { defineField, defineType } from "sanity";

export const arrangor = defineType({
  name: "arrangor",
  title: "Arrangør",
  type: "document",
  icon: BiBuildings,
  fields: [
    defineField({
      name: "selskapsnavn",
      title: "Navn på selskap",
      type: "string",
      validation: (Rule: Rule) => Rule.required(),
    }),
    defineField({
      name: "telefonnummer",
      title: "Telefonnummer",
      type: "string",
    }),
    defineField({
      name: "epost",
      title: "E-post",
      type: "string",
      hidden: true,
    }),
    defineField({
      name: "adresse",
      title: "Adresse",
      type: "string",
      validation: (Rule: Rule) => Rule.required(),
    }),
    defineField({
      name: "organisasjonsnummer",
      title: "Organisasjonsnummer",
      type: "slug",
      validation: (Rule) => Rule.required(),
    }),
  ],
  preview: {
    select: {
      title: "selskapsnavn",
      adresse: "adresse",
      orgnr: "organisasjonsnummer.current",
    },
  },
});
