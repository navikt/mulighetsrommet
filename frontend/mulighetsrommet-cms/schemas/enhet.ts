import { ImOffice } from "react-icons/im";
import { defineType, defineField } from "sanity";

export enum EnhetType {
  Fylke = "Fylke",
  Lokal = "Lokal",
  Als = "Als",
}

export const enhet = defineType({
  name: "enhet",
  title: "Enhet",
  type: "document",
  icon: ImOffice,
  readOnly: true,
  fields: [
    defineField({
      name: "navn",
      title: "Navn",
      type: "string",
      validation: (rule) => rule.required(),
    }),
    defineField({
      name: "nummer",
      title: "Enhetsnummer",
      type: "slug",
      validation: (rule) =>
        rule.required().custom((value) => {
          return (
            /[0-9]{4}/.test(value.current) ||
            "Enhetsnummer is not formatted correctly."
          );
        }),
    }),
    defineField({
      name: "type",
      title: "Type",
      type: "string",
      options: {
        list: Object.values(EnhetType),
      },
      validation: (rule) => rule.required(),
    }),
    defineField({
      name: "status",
      title: "Status",
      type: "string",
      options: {
        list: ["Aktiv", "Nedlagt", "Under utvikling", "Under avvikling"],
      },
      validation: (rule) => rule.required(),
    }),
    defineField({
      name: "fylke",
      title: "Fylke",
      type: "reference",
      to: [{ type: "enhet" }],
      options: {
        filter: "type == $type",
        filterParams: {
          type: EnhetType.Fylke,
        },
      },
      hidden: ({ document }) => {
        return document.type !== EnhetType.Lokal;
      },
    }),
  ],
  preview: {
    select: {
      title: "navn",
      subtitle: "type",
    },
  },
});
