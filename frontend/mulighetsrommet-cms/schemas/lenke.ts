import { defineField } from "sanity";
import { Information } from "../components/Information";

export const lenke = {
  name: "lenke",
  title: "Lenke",
  type: "object",
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
      title: "Lenke",
      name: "lenke",
      type: "url",
      validation: (Rule) => Rule.required().error("Du må lime inn en gyldig url"),
    }),
    defineField({
      title: "Lenkenavn",
      name: "lenkenavn",
      type: "string",
      validation: (Rule) => Rule.required().error("Du må skrive inn et navn for lenken"),
    }),
    defineField({
      title: "Åpne i ny fane?",
      description: "Skru på hvis du vil at lenken skal åpnes i ny fane",
      name: "apneINyFane",
      type: "boolean",
      initialValue: false,
    }),
    defineField({
      title: "Kun tilgjengelig for veileder i Modia?",
      description: "Skru på hvis du vil at lenken bare skal vises for veiledere i Modia",
      name: "visKunForVeileder",
      type: "boolean",
      initialValue: false,
    }),
  ],
  preview: {
    select: {
      title: "lenkenavn",
      subtitle: "lenke",
    },
  },
};
