import { ImStatsDots } from "react-icons/im";

export default {
  name: "statistikk",
  title: "Statistikk",
  type: "document",
  icon: ImStatsDots,
  fields: [
    {
      name: "overskrift",
      title: "Statistikk overskrift",
      type: "string",
      validation: (Rule) =>
        Rule.custom((field, context) =>
          context.document.overskrift && field === undefined
            ? "Dette feltet kan ikke være tomt."
            : true
        ),
    },
    {
      name: "innhold",
      title: "Innhold statistikk",
      type: "string",
      validation: (Rule) =>
        Rule.custom((field, context) =>
          context.document.innhold && field === undefined
            ? "Dette feltet kan ikke være tomt."
            : true
        ),
    },
    {
      name: "hjelpetekst",
      title: "Hjelpetekst til statistikk",
      type: "string",
    },
  ],
  preview: {
    select: {
      title: "overskrift",
    },
  },
};
