import { GrDocument } from "react-icons/gr";
import { defineType } from "sanity";

export default defineType({
  name: "statistikkfil",
  title: "Statistikkfil",
  type: "document",
  icon: GrDocument,
  fields: [
    {
      name: "statistikkfilopplastning",
      title: "Statistikkfilopplastning",
      type: "file",
    },
  ],
});
