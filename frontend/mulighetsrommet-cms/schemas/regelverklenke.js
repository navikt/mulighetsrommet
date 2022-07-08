import { GrLink } from "react-icons/gr";

export default {
  name: "regelverklenke",
  title: "Regelverk lenke",
  type: "document",
  icon: GrLink,
  fields: [
    {
      name: "regelverkUrl",
      title: "Regelverk URL",
      type: "url",
      placeholder: "https://www...",
    },
    {
      name: "regelverkLenkeNavn",
      title: "Navn til lenke",
      description: "Hvilket navn skal vises i lenken?",
      type: "string",
      validation: (Rule) =>
        Rule.custom((field, context) =>
          context.document.regelverkUrl && field === undefined
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
};
