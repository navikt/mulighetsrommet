import { GrUserWorker } from "react-icons/gr";
import { defineField, defineType } from "sanity";
import { Information } from "../components/Information";

export const navKontaktperson = defineType({
  name: "navKontaktperson",
  title: "Nav kontaktperson",
  type: "document",
  icon: GrUserWorker,
  readOnly: true,
  fields: [
    defineField({
      name: "info",
      title: "Info",
      type: "string",
      components: {
        field: () =>
          Information({
            melding:
              "Opprettelse av kontaktpersoner skjer automatisk via baksystem. Kontakt Team Valp på Teams dersom du trenger å få importert kontakter. Vi trenger liste med fullt navn på kontaktpersoner du vil ha her.",
          }),
      },
    }),
    defineField({
      name: "navn",
      title: "Navn",
      type: "string",
      validation: (rule) => rule.required().min(2).max(200),
    }),
    defineField({
      name: "navIdent",
      title: "Nav-ident",
      type: "slug",
      validation: (rule) => rule.required(),
    }),
    defineField({
      name: "enhet",
      title: "Nav-enhet",
      type: "string",
      validation: (rule) => rule.required().min(2).max(200),
    }),
    defineField({
      name: "telefonnummer",
      title: "Telefonnummer",
      type: "string",
    }),
    defineField({
      name: "epost",
      title: "E-post",
      type: "string",
      validation: (rule) => rule.required().min(2).max(200),
    }),
  ],
  preview: {
    select: {
      title: "navn",
      subtitle: "enhet",
    },
  },
});
