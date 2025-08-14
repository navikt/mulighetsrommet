import {
  ExclamationmarkIcon,
  ExclamationmarkTriangleIcon,
  InformationSquareIcon,
} from "@navikt/aksel-icons";
import { defineField, defineType } from "sanity";

export const alertMessage = defineType({
  name: "alertMessage",
  title: "Ekstra oppmerksomhet",
  type: "object",
  fields: [
    defineField({
      name: "variant",
      title: "Variant",
      type: "array",
      validation: (Rule) => Rule.required().length(1).error("Du må velge én variant"),
      of: [{ type: "string" }],
      options: {
        list: [
          { title: "Informasjon - Tekst på blå bakgrunn for veileder", value: "info" },
          {
            title: "Obs obs - Tekst på oransje bakgrunn for veileder",
            value: "warning",
          },
          {
            title: "Vær ekstra oppmerksom her - Tekst på rød bakgrunn for veileder",
            value: "error",
          },
        ],
      },
    }),
    defineField({
      name: "innhold",
      title: "Innhold",
      type: "alertContent",
      validation: (Rule) => Rule.required().error("Du må oppgi innhold"),
    }),
  ],
  preview: {
    select: {
      variant: "variant",
      body: "innhold",
    },
    prepare({ variant, body }) {
      let bodyText = "";
      if (Array.isArray(body)) {
        const block = body.find((b) => b._type === "block");
        if (block?.children) {
          bodyText = block.children.map((child) => child.text).join("");
        }
      }

      const variantValue = Array.isArray(variant) ? variant[0] : variant;
      let icon;
      switch (variantValue) {
        case "info":
          icon = InformationSquareIcon;
          break;
        case "warning":
          icon = ExclamationmarkIcon;
          break;
        case "error":
          icon = ExclamationmarkTriangleIcon;
          break;
        default:
          icon = InformationSquareIcon;
      }

      return {
        title: bodyText,
        icon: icon,
      };
    },
  },
});
