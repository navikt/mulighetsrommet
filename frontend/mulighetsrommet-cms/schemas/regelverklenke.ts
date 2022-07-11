import { GrLink } from "react-icons/gr";
import { defineType } from "sanity";

export default defineType({
  name: "regelverklenke",
  title: "Regelverk lenke",
  type: "document",
  icon: GrLink,
  fields: [
    {
      name: "regelverkUrl",
      title: "Regelverk URL",
      type: "url",
      options: {
        placeholder: "https://www...",
      },
    },
    {
      name: "regelverkLenkeNavn",
      title: "Navn til lenke",
      description: "Hvilket navn skal vises i lenken?",
      type: "string",
      validation: (Rule) =>
        Rule.custom((field, { document }) =>
          document?.regelverkUrl && field === undefined
            ? "Dette feltet kan ikke være tomt."
            : true
        ),
      hidden: ({ document }) => !document?.regelverkUrl,
    },
  ],
  preview: {
    select: {
      title: "regelverkLenkeNavn",
    },
  },
});
