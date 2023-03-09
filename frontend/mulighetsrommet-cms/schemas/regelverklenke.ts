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
      name: "info",
      title: "Info",
      type: "string",
      components: {
        field: Information,
      },
    }),
    defineField({
      name: "regelverkUrl",
      title: "Regelverk URL",
      type: "url",
      placeholder: "https://www...",
      validation: (rule) => rule.required(),
    }),
    defineField({
      name: "regelverkLenkeNavn",
      title: "Navn til lenke",
      description: "Hvilken tekst som skal vises i lenken.",
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
        "Beskrivelse av hva lenken brukes til eller omhandler. Beskrivelsen vises ikke til veiledere, den er kun for internt bruk i Sanity.",
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
