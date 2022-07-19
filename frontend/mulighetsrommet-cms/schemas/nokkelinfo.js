import { FiKey } from "react-icons/fi";

export default {
  name: "nokkelinfo",
  title: "Nøkkelinfo",
  type: "object",
  icon: FiKey,
  fields: [
    {
      name: "overskrift",
      title: "Nøkkelinfo overskrift",
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
      title: "Innhold nokkelinfo",
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
      title: "Hjelpetekst til nokkelinfo",
      type: "string",
    },
  ],
  preview: {
    select: {
      title: "overskrift",
    },
  },
};
