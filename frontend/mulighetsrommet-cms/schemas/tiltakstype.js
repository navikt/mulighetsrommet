export default {
  name: "tiltakstype",
  title: "Tiltakstype",
  type: "document",
  fields: [
    {
      name: "tiltakskode",
      title: "Tiltakskode",
      type: "slug",
      description: "Tiltakskode fra Arena, f.eks. 'INDOPPFAG'.",
      validation: (Rule) => Rule.required(),
    },
    {
      name: "title",
      title: "Tittel",
      type: "string",
      validation: (Rule) => Rule.required(),
    },
    {
      name: "promoImage",
      title: "Promobilde",
      type: "image",
      options: {
        hotspot: true,
      },
    },
    {
      name: "about",
      title: "Om tiltaket",
      type: "blockContent",
    },
  ],
  preview: {
    select: {
      title: "title",
      media: "promoImage",
    },
  },
};
