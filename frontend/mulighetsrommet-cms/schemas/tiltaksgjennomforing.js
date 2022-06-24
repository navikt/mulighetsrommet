import { GrDocumentPerformance } from "react-icons/gr";
import sanityClient from "part:@sanity/base/client";
import { EnhetType } from "./enhet";

const client = sanityClient.withConfig({ apiVersion: "2021-10-21" });

export default {
  name: "tiltaksgjennomforing",
  title: "Tiltaksgjennomføring",
  type: "document",
  icon: GrDocumentPerformance,
  fields: [
    {
      name: "tiltakstype",
      title: "Tiltakstype",
      type: "reference",
      to: [{ type: "tiltakstype" }],
      validation: (Rule) => Rule.required(),
    },
    {
      name: "tiltaksgjennomforingNavn",
      title: "Navn på tiltaksgjennomføring",
      type: "string",
      validation: (Rule) => Rule.required(),
    },
    {
      name: "beskrivelse",
      title: "Beskrivelse",
      type: "string",
    },
    {
      name: "tiltaksnummer",
      title: "Tiltaksnummer",
      type: "number",
      validation: (Rule) => Rule.required(),
    },
    {
      name: "kontaktinfoArrangor",
      title: "Arrangør",
      type: "reference",
      to: [{ type: "arrangor" }],
      validation: (Rule) => Rule.required(),
    },
    {
      name: "lokasjon",
      title: "Lokasjon",
      type: "string",
      validation: (Rule) => Rule.required(),
    },
    {
      name: "fylke",
      title: "Fylke",
      description: "I hvilken region gjelder dette tiltaket?",
      type: "reference",
      to: [{ type: "enhet" }],
      options: {
        disableNew: true,
        filter: "type == $type",
        filterParams: {
          type: EnhetType.Fylke,
        },
      },
      validation: (Rule) => Rule.required(),
    },
    {
      name: "enheter",
      title: "Enheter",
      description:
        "Hvilke enheter kan benytte seg av dette tiltaket? Hvis det gjelder for hele regionen kan dette stå tomt.",
      type: "array",
      hidden: ({ document }) => {
        return !document.fylke;
      },
      of: [
        {
          type: "reference",
          to: [{ type: "enhet" }],
          options: {
            disableNew: true,
            filter: ({ document }) => {
              return {
                filter: `fylke._ref == $fylke`,
                params: {
                  fylke: document.fylke._ref,
                },
              };
            },
          },
        },
      ],
      validation: (Rule) =>
        Rule.unique().custom(async (enheter, { document }) => {
          if (!document.fylke) {
            return true;
          }

          const validEnheter = await client.fetch(
            "*[_type == 'enhet' && fylke._ref == $fylke]._id",
            { fylke: document.fylke._ref }
          );

          const paths = enheter
            .filter((enhet) => !validEnheter.includes(enhet._ref))
            .map((enhet) => [{ _key: enhet._key }]);

          return !paths.length
            ? true
            : { message: "Alle enheter må tilhøre valgt fylke", paths };
        }),
    },
    {
      name: "oppstart",
      title: "Oppstart",
      type: "string",
      options: {
        list: [
          { title: "Dato", value: "dato" },
          { title: "Løpende", value: "lopende" },
        ],
      },
      validation: (Rule) => Rule.required(),
    },
    {
      name: "oppstartsdato",
      title: "Oppstart dato",
      type: "date",
      options: { dateFormat: "DD/MM/YYYY" },
      hidden: ({ parent }) => parent?.oppstart !== "dato",
    },
    //Faneinnhold
    {
      name: "faneinnhold",
      title: "Innhold faner",
      type: "object",
      fields: [
        {
          name: "forHvemInfoboks",
          title: "For hvem - infoboks",
          description:
            "Hvis denne har innhold, vises det i en infoboks i fanen 'For hvem'",
          type: "string",
        },
        {
          name: "forHvem",
          title: "For hvem",
          type: "blockContent",
        },

        {
          name: "detaljerOgInnholdInfoboks",
          title: "Detaljer og innhold - infoboks",
          description:
            "Hvis denne har innhold, vises det i en infoboks i fanen 'Detaljer og innhold'",
          type: "string",
        },
        {
          name: "detaljerOgInnhold",
          title: "Detaljer og innhold",
          type: "blockContent",
        },

        {
          name: "pameldingOgVarighetInfoboks",
          title: "Påmelding og varighet - infoboks",
          description:
            "Hvis denne har innhold, vises det i en infoboks i fanen 'Påmelding og varighet'",
          type: "string",
        },
        {
          name: "pameldingOgVarighet",
          title: "Påmelding og varighet",
          type: "blockContent",
        },
      ],
    },
    //TODO skal kunne legge til flere tiltaksansvarlige
    {
      name: "kontaktinfoTiltaksansvarlige",
      title: "Tiltaksansvarlig",
      type: "array",
      of: [{ type: "reference", to: [{ type: "navKontaktperson" }] }],
      validation: (Rule) => Rule.required().min(1).unique(),
    },
    {
      name: "lenker",
      title: "Lenker",
      type: "array",
      of: [
        {
          type: "object",
          fields: [
            {
              title: "Lenke",
              name: "lenke",
              type: "string",
            },
            {
              title: "Lenkenavn",
              name: "lenkenavn",
              type: "string",
            },
          ],
        },
      ],
    },
  ],
  preview: {
    select: {
      title: "tiltaksgjennomforingNavn",
      tiltakstypeNavn: "tiltakstype.tiltakstypeNavn",
    },
    prepare: (selection) => {
      const { title, tiltakstypeNavn } = selection;
      return {
        title,
        subtitle: `Tiltakstype: ${tiltakstypeNavn}`,
      };
    },
  },
};
