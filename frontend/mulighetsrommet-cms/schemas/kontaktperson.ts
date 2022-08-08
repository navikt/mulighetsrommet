import { GrUserWorker } from "react-icons/gr";
import { Rule } from "@sanity/types";

export default {
  name: "navKontaktperson",
  title: "NAV kontaktperson",
  type: "document",
  icon: GrUserWorker,
  fields: [
    {
      name: "navn",
      title: "Navn",
      type: "string",
      validation: (Rule: Rule) => Rule.required().min(2).max(200),
    },
    {
      name: "enhet",
      title: "NAV-enhet",
      type: "string",
      validation: (Rule: Rule) => Rule.required().min(2).max(200),
    },
    {
      name: "telefonnummer",
      title: "Telefonnummer",
      type: "string",
      validation: (Rule: Rule) => Rule.required().min(2).max(200),
    },
    {
      name: "epost",
      title: "E-post",
      type: "string",
      validation: (Rule: Rule) => Rule.required().min(2).max(200),
    },
  ],
  preview: {
    select: {
      title: "navn",
    },
  },
};
