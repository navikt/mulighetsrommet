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
          { title: "Informasjon", value: "info" },
          { title: "Obs obs", value: "warning", description: "testing" },
          { title: "Vær ekstra oppmerksom her", value: "error" },
        ],
      },
    }),
    defineField({
      name: "innhold",
      title: "Innhold",
      type: "oppskriftContent",
      validation: (Rule) => Rule.required().error("Du må oppgi innhold"),
    }),
  ],
});
