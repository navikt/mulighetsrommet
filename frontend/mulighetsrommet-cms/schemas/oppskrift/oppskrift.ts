import { defineField, defineType } from "sanity";

export const oppskrift = defineType({
  name: "oppskrift",
  title: "Oppskrift",
  type: "document",
  fields: [
    defineField({
      name: "navn",
      title: "Navn på oppskrift",
      description: "Gi oppskriften en beskrivende tittel",
      validation: (Rule) => Rule.required().min(5).max(100).error("Du må gi oppskriften et navn"),
      type: "string",
    }),
    defineField({
      name: "beskrivelse",
      title: "Beskrivelse av oppskriften",
      description: "Gi en god beskrivelse så kollegaer forstår hva oppskriften inneholder",
      validation: (Rule) =>
        Rule.required().min(5).max(250).error("Du må gi oppskriften en beskrivelse"),
      type: "text",
    }),
    defineField({
      name: "steg",
      title: "Steg i oppskriften",
      validation: (Rule) =>
        Rule.required().min(1).error("Du må fylle inn minst ett steg i oppskriften"),
      type: "array",
      of: [{ type: "steg" }],
    }),
  ],
});
