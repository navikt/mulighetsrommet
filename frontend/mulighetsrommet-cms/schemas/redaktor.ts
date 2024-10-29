import { GrUserAdmin } from "react-icons/gr";
import { defineField, defineType } from "sanity";

export const redaktor = defineType({
  name: "redaktor",
  title: "Administrator",
  type: "document",
  icon: GrUserAdmin,
  readOnly: true,
  fields: [
    defineField({
      name: "navn",
      title: "Navn",
      type: "string",
      validation: (rule) => rule.required().min(2).max(200),
      initialValue: (params, { currentUser }) => {
        const { name } = currentUser;
        return name;
      },
    }),
    defineField({
      name: "navIdent",
      title: "Nav-ident",
      type: "slug",
      validation: (rule) => rule.required(),
    }),
    defineField({
      name: "epost",
      title: "Nav-epost",
      type: "slug",
      validation: (rule) => rule.required(),
      initialValue: async (params, { currentUser }) => {
        const { email } = currentUser;
        return { _type: "slug", current: email };
      },
    }),
    defineField({
      name: "enhet",
      title: "Nav-enhet",
      description: "Tilhørende Nav-enhet.",
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
