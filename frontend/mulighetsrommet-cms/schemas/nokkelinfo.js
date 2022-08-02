import { FiKey } from "react-icons/fi";

export default {
  name: "nokkelinfo",
  title: "Nøkkelinfo",
  type: "object",
  icon: FiKey,
  fields: [
    {
      name: "tittel",
      title: "Nøkkelinfo tittel",
      type: "string",
      validation: (Rule) =>
        Rule.custom((field, context) =>
          context.document.tittel && field === undefined
            ? "Dette feltet kan ikke være tomt."
            : true
        ),
    },
    {
      name: "innhold",
      title: "Innhold nøkkelinfo",
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
      title: "Hjelpetekst til nøkkelinfo",
      type: "string",
    },
  ],
  preview: {
    select: {
      title: "tittel",
    },
  },
};
