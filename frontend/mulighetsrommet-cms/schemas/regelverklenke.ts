import { GrLink } from "react-icons/gr";
import { Rule } from "@sanity/types";

export default {
  name: "regelverklenke",
  title: "Regelverkslenke",
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
      validation: (Rule: Rule) =>
        Rule.custom((field, context) =>
          context.document.regelverkUrl && field === undefined
            ? "Dette feltet kan ikke være tomt."
            : true
        ),
      hidden: ({ document }) => !document?.regelverkUrl,
    },
    {
      name: "beskrivelse",
      title: "Valgfri beskrivelse av lenken",
      description:
        "En beskrivelse som forteller andre redaktører hva lenken brukes til eller omhandler. Beskrivelsen vises ikke til veiledere, den er kun for intern bruk her i Sanity.",
      type: "text",
      rows: 1,
      validation: (Rule) => Rule.max(100),
    },
  ],
  preview: {
    select: {
      title: "regelverkLenkeNavn",
      subtitle: "beskrivelse",
    },
  },
};
