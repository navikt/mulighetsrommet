import { BiBuildings } from "react-icons/bi";
import { defineField, defineType } from "sanity";
import { Information } from "../components/Information";

export const arrangor = defineType({
  name: "arrangor",
  title: "Arrangør",
  type: "document",
  icon: BiBuildings,
  fields: [
    defineField({
      name: "information",
      title: " ",
      type: "string",
      components: {
        input: Information,
      },
    }),
    defineField({
      name: "selskapsnavn",
      title: "Navn på selskap",
      type: "string",
      validation: (rule) => rule.required(),
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
      validation: (rule) => rule.required(),
    }),
    defineField({
      name: "organisasjonsnummer",
      title: "Organisasjonsnummer",
      type: "slug",
      validation: (rule) => rule.required(),
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
