import { defineField, defineType } from "sanity";

export const oppskriftContent = defineType({
  title: "Innhold for oppskrift",
  name: "oppskriftContent",
  type: "array",
  of: [
    {
      title: "Block",
      type: "block",
      // Styles let you set what your user can mark up blocks with. These
      // correspond with HTML tags, but you can set any title or value
      // you want and decide how you want to deal with it where you want to
      // use your content.
      styles: [
        { title: "Normal", value: "normal" },
        { title: "Overskrift", value: "h3" },
      ],
      lists: [{ title: "Bullet", value: "bullet" }],
      // Marks let you mark up inline text in the block editor.
      marks: {
        // Decorators usually describe a single property – e.g. a typographic
        // preference or highlighting by editors.
        decorators: [
          { title: "Strong", value: "strong" },
          { title: "Emphasis", value: "em" },
        ],
        // Annotations can be any object structure – e.g. a link or a footnote.
        annotations: [
          {
            title: "URL",
            name: "link",
            type: "object",
            fields: [
              {
                title: "URL",
                name: "href",
                type: "url",
              },
            ],
          },
        ],
      },
    },
    {
      type: "image",
      fields: [
        defineField({
          type: "text",
          name: "altText",
          title: "Alt-tekst",
          description:
            "En god alternativ tekst gjør at brukere med skjermleser (feks. blinde) også vet hva bildet representerer.",
          validation: (Rule) =>
            Rule.required().error(
              "Alle bilder brukt må ha en god, beskrivende alt-tekst slik at blinde brukere får en god brukeropplevelse.",
            ),
        }),
      ],
    },
    {
      type: "tips",
    },
    { type: "alertMessage" },
  ],
});
