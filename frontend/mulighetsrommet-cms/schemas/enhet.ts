import { Rule } from "@sanity/types";
import { ConditionalPropertyCallbackContext } from "@sanity/types/src/schema/types";
import { CustomValidatorResult } from "@sanity/types/src/validation/types";

export enum EnhetType {
  Fylke = "Fylke",
  Lokal = "Lokal",
}

export default {
  name: "enhet",
  title: "Enhet",
  type: "document",
  readOnly: true,
  fields: [
    {
      name: "navn",
      title: "Navn",
      type: "string",
      validation: (rule: Rule) => rule.required(),
    },
    {
      name: "nummer",
      title: "Enhetsnummer",
      type: "slug",
      validation: (rule: Rule) =>
        rule
          .required()
          .custom<{ current?: string }>((value): CustomValidatorResult => {
            return (
              /[0-9]{4}/.test(value.current) ||
              "Enhetsnummer is not formatted correctly."
            );
          }),
    },
    {
      name: "type",
      title: "Type",
      type: "string",
      options: {
        list: Object.values(EnhetType),
      },
      validation: (rule: Rule) => rule.required(),
    },
    {
      name: "status",
      title: "Status",
      type: "string",
      options: {
        list: ["Aktiv", "Nedlagt", "Under utvikling", "Under avvikling"],
      },
      validation: (rule: Rule) => rule.required(),
    },
    {
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
      hidden: ({ document }: ConditionalPropertyCallbackContext) => {
        return document.type !== EnhetType.Lokal;
      },
    },
  ],
  preview: {
    select: {
      title: "navn",
      subtitle: "type",
    },
  },
};
