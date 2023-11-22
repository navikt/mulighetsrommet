import { defineField, defineType } from "sanity";

export const steg = defineType({
  name: "steg",
  title: "Steg",
  type: "object",
  fields: [
    defineField({
      name: "navn",
      title: "Navn på steget",
      type: "string",
      validation: (Rule) => Rule.required().min(5).max(100).error("Du må gi steget et navn"),
    }),
    defineField({
      name: "innhold",
      title: "Innhold",
      type: "oppskriftContent",
      description: "Husk å skrive med et enkelt språk så veiledere forstår hva de skal gjøre",
      validation: (Rule) => Rule.required().error("Et steg må ha noe innhold"),
    }),
  ],
});
