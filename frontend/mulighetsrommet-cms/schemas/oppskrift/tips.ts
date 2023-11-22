import { defineField, defineType } from "sanity";

export const tips = defineType({
  name: "tips",
  title: "Tips",
  type: "object",
  fields: [
    defineField({
      name: "innhold",
      title: "Innhold",
      type: "oppskriftContent",
    }),
  ],
});
