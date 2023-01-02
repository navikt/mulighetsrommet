import { GrLink } from "react-icons/gr";
import { defineField, defineType } from "sanity";
import { Information } from "../components/Information";

export const regelverklenke = defineType({
  name: "regelverklenke",
  title: "Regelverkslenke",
  type: "document",
  icon: GrLink,
  fields: [
    defineField({
      name: "information",
      title: " ",
      type: "string",
      components: {
        input: Information,
      },
    }),
    defineField({
      name: "regelverkUrl",
      title: "Regelverk URL",
      type: "url",
      placeholder: "https://www...",
    }),
    defineField({
      name: "regelverkLenkeNavn",
      title: "Navn til lenke",
      description: "Her velger du hvilken tekst som skal vises i lenken.",
      type: "string",
      validation: (rule) =>
        rule.custom((field, context) =>
          context.document.regelverkUrl && field === undefined
            ? "Dette feltet kan ikke være tomt."
            : true
        ),
      hidden: ({ document }) => !document?.regelverkUrl,
    }),
    defineField({
      name: "beskrivelse",
      title: "Valgfri beskrivelse av lenken",
      description:
        "Her skriver du en beskrivelse om hva lenken brukes til eller omhandler. Beskrivelsen vises ikke til veiledere, den er kun for intern bruk i Sanity.",
      type: "text",
      rows: 1,
      validation: (rule) => rule.max(100),
    }),
  ],
  preview: {
    select: {
      title: "regelverkLenkeNavn",
      subtitle: "beskrivelse",
    },
  },
});
