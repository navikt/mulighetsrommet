import { GrUserAdmin } from "react-icons/gr";
import { Rule } from "@sanity/types";

export default {
  name: "redaktor",
  title: "Redaktør",
  type: "document",
  icon: GrUserAdmin,
  fields: [
    {
      name: "navn",
      title: "Navn",
      description:
        "Brukes for å filtrere på tiltaksgjennomføringene du oppretter.",
      type: "string",
      validation: (Rule: Rule) => Rule.required().min(2).max(200),
    },
    {
      name: "enhet",
      title: "NAV-enhet",
      description: "Hvilken NAV-enhet du tilhører",
      type: "string",
      validation: (Rule: Rule) => Rule.required().min(2).max(200),
    },
  ],
  preview: {
    select: {
      title: "navn",
      subtitle: "enhet",
    },
  },
};
