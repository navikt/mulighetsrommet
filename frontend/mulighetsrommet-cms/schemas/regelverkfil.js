import { GrDocument } from "react-icons/gr";

export default {
  name: "regelverkfil",
  title: "Regelverk fil",
  type: "document",
  icon: GrDocument,
  fields: [
    {
      name: "regelverkFilOpplastning",
      title: "Regelverk fil opplastning",
      type: "file",
    },
    {
      name: "regelverkFilNavn",
      title: "Navn til fil",
      description: "Hvilket navn skal vises til filen?",
      type: "string",
      validation: (Rule) =>
          Rule.custom((field, context) =>
              context.document.regelverkFilOpplastning && field === undefined
                  ? "Dette feltet kan ikke være tomt."
                  : true
          ),
      hidden: ({ document }) => !document?.regelverkFilOpplastning,
    },
  ],
  preview: {
    select: {
      title: "regelverkFilNavn",
    },
  },
};
