import { GrDocument } from "react-icons/gr";

export default {
  name: "statistikk",
  title: "Statistikk",
  type: "document",
  icon: GrDocument,
  fields: [
    {
      name: "statistikkOverskrift",
      title: "Statistikk overskrift",
      type: "string",
      validation: (Rule) =>
        Rule.custom((field, context) =>
          context.document.statistikkOverskrift && field === undefined
            ? "Dette feltet kan ikke være tomt."
            : true
        ),
    },
    {
      name: "statistikkInnhold",
      title: "Innhold statistikk",
      type: "string",
      validation: (Rule) =>
        Rule.custom((field, context) =>
          context.document.statistikkInnhold && field === undefined
            ? "Dette feltet kan ikke være tomt."
            : true
        ),
    },
    {
      name: "statistikkHjelpetekst",
      title: "Hjelpetekst til statistikk",
      type: "string",
    },
  ],
  preview: {
    select: {
      title: "statistikkOverskrift",
    },
  },
};
