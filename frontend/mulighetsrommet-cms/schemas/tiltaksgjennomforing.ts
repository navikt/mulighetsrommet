import { GrDocumentPerformance } from "react-icons/gr";
import { EnhetType } from "./enhet";
import { lenke } from "./lenke";
import { defineField, defineType } from "sanity";
import { API_VERSION } from "../sanity.config";
import { Information } from "../components/Information";

export const tiltaksgjennomforing = defineType({
  name: "tiltaksgjennomforing",
  title: "Tiltaksgjennomføring",
  type: "document",
  icon: GrDocumentPerformance,
  fields: [
    defineField({
      name: "info",
      title: "Info",
      type: "string",
      components: {
        field: Information,
      },
    }),
    defineField({
      name: "redaktor",
      title: "Redaktører",
      type: "array",
      description: "Eier av innholdet i denne tiltaksgjennomføringen.",
      of: [
        {
          type: "reference",
          to: [{ type: "redaktor" }],
        },
      ],
      validation: (rule) => rule.required().unique(),
      initialValue: async (params, { currentUser, getClient }) => {
        const foundRedaktor = await getClient({
          apiVersion: API_VERSION,
        }).fetch(`*[_type == "redaktor" && navn == '${currentUser.name}'][0]`);
        if (!foundRedaktor) return [];
        return [
          {
            _type: "reference",
            _ref: foundRedaktor?._id,
          },
        ];
      },
    }),
    defineField({
      name: "tiltakstype",
      title: "Tiltakstype",
      type: "reference",
      to: [{ type: "tiltakstype" }],
      validation: (rule) => rule.required(),
    }),
    defineField({
      name: "tiltaksgjennomforingNavn",
      title: "Navn på tiltaksgjennomføring",
      type: "string",
      validation: (rule) => rule.required(),
    }),
    defineField({
      name: "aar",
      title: "År",
      description:
        "Hvis tiltakstypen gjelder individuelle tiltak skal du ikke fylle inn år.",
      type: "number",
      initialValue: () => new Date().getFullYear(),
    }),
    defineField({
      name: "lopenummer",
      title: "Løpenummer",
      description:
        "Hvis tiltakstypen gjelder individuelle tiltak skal du ikke fylle inn løpenummer.",
      type: "number",
    }),
    defineField({
      name: "tiltaksnummer",
      title: "Tiltaksnummer",
      description:
        "Hvis tiltakstypen gjelder individuelle tiltak skal du ikke fylle inn noe her. Tiltaksnummer utledes fra feltene år og løpenummer over",
      type: "slug",
      options: {
        slugify: (input) => {
          return input;
        },
        source: (doc, _) => {
          const aar = doc.aar as unknown as string;
          const lopenummer = doc.lopenummer as unknown as string;
          return `${aar ? aar : new Date().getFullYear()}#${
            lopenummer ? lopenummer : 0
          }`;
        },
      },
    }),
    defineField({
      name: "kontaktinfoArrangor",
      title: "Arrangør",
      description:
        "Ikke velg arrangør dersom tiltakstypen gjelder individuelle tiltak.",
      type: "reference",
      to: [{ type: "arrangor" }],
    }),
    defineField({
      name: "beskrivelse",
      title: "Beskrivelse",
      description: "Beskrivelse av formålet med tiltaksgjennomføringen.",
      type: "text",
      rows: 5,
      validation: (rule) => rule.max(500),
    }),

    defineField({
      name: "tilgjengelighetsstatus",
      title: "Tilgjengelighetsstatus",
      description:
        "Tilgjengelighetsstatus utledes fra data i Arena og kan ikke overskrives i Sanity.",
      readOnly: true,
      type: "string",
      options: {
        list: [
          { title: "Åpent", value: "Ledig" },
          { title: "Venteliste", value: "Venteliste" },
          { title: "Stengt", value: "Stengt" },
        ],
      },
    }),
    defineField({
      name: "estimert_ventetid",
      title: "Merknad til tilgjengelighetsstatus",
      description: "F.eks estimert ventetid eller stengt til dato.",
      type: "string",
    }),
    defineField({
      name: "oppstart",
      title: "Oppstart eller midlertidig stengt",
      type: "string",
      options: {
        list: [
          { title: "Dato", value: "dato" },
          { title: "Løpende oppstart", value: "lopende" },
          { title: "Midlertidig stengt", value: "midlertidig_stengt" },
        ],
      },
      validation: (rule) => rule.required(),
    }),
    defineField({
      name: "oppstartsdato",
      title: "Dato for oppstart",
      type: "date",
      options: { dateFormat: "DD/MM/YYYY" },
      hidden: ({ parent }) => parent?.oppstart !== "dato",
    }),
    defineField({
      name: "sluttdato",
      title: "Sluttdato",
      description: "Dato for når gjennomføringen slutter",
      type: "date",
      options: { dateFormat: "DD/MM/YYYY" },
    }),
    defineField({
      name: "lokasjon",
      title: "Lokasjon",
      description:
        "Sted for gjennomføring, f.eks. Fredrikstad eller Tromsø. Veileder kan filtrere på verdiene i dette feltet, så ikke skriv fulle adresser.",
      type: "string",
      validation: (rule) => rule.required(),
    }),
    defineField({
      name: "fylke",
      title: "Fylke",
      description: "Hvilket fylke gjelder tiltaket for.",
      type: "reference",
      to: [{ type: "enhet" }],
      options: {
        disableNew: true,
        filter: "type == $type",
        filterParams: {
          type: EnhetType.Fylke,
        },
      },
      validation: (rule) => rule.required(),
    }),
    defineField({
      name: "enheter",
      title: "Enheter",
      description:
        "Hvilke enheter kan benytte seg av dette tiltaket? Hvis det gjelder for hele regionen kan dette feltet stå tomt.",
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
      validation: (rule) =>
        rule.required().custom(async (enheter, { document, getClient }) => {
          if (!document.fylke || !enheter) {
            return true;
          }

          const validEnheter = await getClient({
            apiVersion: API_VERSION,
          }).fetch("*[_type == 'enhet' && fylke._ref == $fylke]._id", {
            fylke: document.fylke._ref,
          });

          const paths = enheter
            ?.filter((enhet) => !validEnheter.includes(enhet._ref))
            ?.map((enhet) => [{ _key: enhet._key }]);

          return !paths.length
            ? true
            : { message: "Alle enheter må tilhøre valgt fylke.", paths };
        }),
    }),

    //Faneinnhold
    defineField({
      name: "faneinnhold",
      title: "Faneinnhold",
      type: "faneinnhold",
    }),
    defineField({
      name: "kontaktinfoTiltaksansvarlige",
      title: "Tiltaksansvarlig",
      description: "Tiltaksansvarlige for tiltaksgjennomføringen.",
      type: "array",
      of: [{ type: "reference", to: [{ type: "navKontaktperson" }] }],
      validation: (rule) => rule.required().min(1).unique(),
    }),
    defineField({
      name: "lenker",
      title: "Lenker",
      description:
        "Dersom du har lenker som er interessant for tiltaksgjennomføringen kan det legges til her. PS: Per 05.10.2022 er dette feltet ikke synlig for veiledere enda.",
      type: "array",
      of: [
        {
          type: "lenke",
        },
      ],
      hidden: true, // Skjules per 25.10.22 etter ønske fra Marthe pga. forvirring for redaktørene.
    }),
  ],
  orderings: [
    {
      title: "Tiltakstype A->Å",
      name: "Tiltakstype A->Å",
      by: [
        {
          field: "tiltakstype.tiltakstypeNavn",
          direction: "asc",
        },
      ],
    },
    {
      title: "Tiltakstype Å->A",
      name: "Tiltakstype Å->A",
      by: [
        {
          field: "tiltakstype.tiltakstypeNavn",
          direction: "desc",
        },
      ],
    },
    {
      title: "Tiltaksnavn A->Å",
      name: "Tiltaksnavn A->Å",
      by: [
        {
          field: "tiltaksgjennomforingNavn",
          direction: "asc",
        },
      ],
    },
    {
      title: "Tiltaksnavn Å->A",
      name: "Tiltaksnavn Å->A",
      by: [
        {
          field: "tiltaksgjennomforingNavn",
          direction: "desc",
        },
      ],
    },
  ],
  preview: {
    select: {
      title: "tiltaksgjennomforingNavn",
      tiltaksnummer: "tiltaksnummer.current",
    },
    prepare: ({ title, tiltaksnummer }) => ({
      title,
      subtitle: tiltaksnummer ? tiltaksnummer : "",
    }),
  },
});
