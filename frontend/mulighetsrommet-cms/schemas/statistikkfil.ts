import { ImStatsDots } from "react-icons/im";
import { defineType, defineField } from "sanity";

export const statistikkfil = defineType({
  name: "statistikkfil",
  title: "Statistikkfil",
  type: "document",
  icon: ImStatsDots,
  fields: [
    defineField({
      name: "statistikkfilopplastning",
      title: "Statistikkfilopplastning",
      type: "file",
    }),
  ],
});
