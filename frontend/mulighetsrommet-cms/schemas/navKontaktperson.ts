import { GrUserWorker } from "react-icons/gr";
import { Rule } from "@sanity/types";
import { defineType, defineField } from "sanity";

export const navKontaktperson = defineType({
  name: "navKontaktperson",
  title: "NAV kontaktperson",
  type: "document",
  icon: GrUserWorker,
  fields: [
    defineField({
      name: "navn",
      title: "Navn",
      type: "string",
      validation: (Rule: Rule) => Rule.required().min(2).max(200),
    }),
    defineField({
      name: "enhet",
      title: "NAV-enhet",
      type: "string",
      validation: (Rule: Rule) => Rule.required().min(2).max(200),
    }),
    defineField({
      name: "telefonnummer",
      title: "Telefonnummer",
      type: "string",
      validation: (Rule: Rule) => Rule.required().min(2).max(200),
    }),
    defineField({
      name: "epost",
      title: "E-post",
      type: "string",
      validation: (Rule: Rule) => Rule.required().min(2).max(200),
    }),
  ],
  preview: {
    select: {
      title: "navn",
    },
  },
});
