import { GrDocumentPerformance } from "react-icons/gr";
import sanityClient from "part:@sanity/base/client";
import { Rule } from "@sanity/types";
import { EnhetType } from "./enhet";
import lenke from "./lenke";

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
      description:
        "Her velger du hvilken tiltakstype gjennomføringen gjelder for",
      to: [{ type: "tiltakstype" }],
      validation: (Rule: Rule) => Rule.required(),
    },
    {
      name: "tiltaksgjennomforingNavn",
      title: "Navn på tiltaksgjennomføring",
      description: "Her legger du inn navn for tiltaksgjennomføringen",
      type: "string",
      validation: (Rule: Rule) => Rule.required(),
    },
    {
      name: "beskrivelse",
      title: "Beskrivelse",
      description:
        "Her kan du legge til en tekstlig beskrivelse av tiltaksgjennomføringen",
      type: "string",
    },
    {
      name: "tiltaksnummer",
      title: "Tiltaksnummer",
      description: "Her skriver du inn tiltaksnummeret for gjennomføringen",
      type: "number",
      validation: (Rule: Rule) => Rule.required(),
    },
    {
      name: "estimert_ventetid",
      title: "Estimert ventetid",
      description:
        "Her kan du oppgi estimert ventetid for tiltaket. Det kan være lurt å sjekke at dette feltet stemmer dersom det er lagt inn en estimert ventetid og ventetiden endrer seg gjennom året.",
      type: "string",
    },
    {
      name: "kontaktinfoArrangor",
      title: "Arrangør",
      description:
        "Ikke velg arrangør dersom tiltakstypen gjelder individuelle tiltak",
      type: "reference",
      to: [{ type: "arrangor" }],
      validation: (Rule) =>
        Rule.custom(async (arrangor, { document }) => {
          const tiltaksgruppe = await client.fetch(
            "*[_type == 'tiltakstype' && _id == $tiltakstype].tiltaksgruppe",
            { tiltakstype: document.tiltakstype._ref }
          );

          if (tiltaksgruppe?.includes("individuelt")) {
            if (arrangor) {
              return "Individuelle tiltak skal ikke ha noen arrangør";
            }
            return true;
          }

          if (!arrangor) {
            return "For tiltak som ikke er individuelle må man velge en arrangør";
          }

          return true;
        }),
    },
    {
      name: "lokasjon",
      title: "Lokasjon",
      description:
        "Her skriver du inn hvor i tiltaket gjelder. Feks. Fredrikstad eller Tromsø. Veileder kan filtrere på verdiene i dette feltet, så ikke skriv fulle adresser.",
      type: "string",
      validation: (Rule: Rule) => Rule.required(),
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
      validation: (Rule: Rule) => Rule.required(),
    },
    {
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
      validation: (Rule: Rule) =>
        Rule.unique().custom(async (enheter, { document }) => {
          if (!document.fylke || !enheter) {
            return true;
          }

          const validEnheter = await client.fetch(
            "*[_type == 'enhet' && fylke._ref == $fylke]._id",
            { fylke: document.fylke._ref }
          );

          const paths = enheter
            ?.filter((enhet) => !validEnheter.includes(enhet._ref))
            ?.map((enhet) => [{ _key: enhet._key }]);

          return !paths.length
            ? true
            : { message: "Alle enheter må tilhøre valgt fylke", paths };
        }),
    },
    {
      name: "oppstart",
      title: "Oppstart eller midlertidig stengt",
      description:
        "Her velger du om tiltaksgjennomføringen har oppstart på en spesifikk dato eller om det er løpende oppstart.",
      type: "string",
      options: {
        list: [
          { title: "Dato", value: "dato" },
          { title: "Løpende", value: "lopende" },
          { title: "Midlertidig stengt", value: "midlertidig_stengt" },
        ],
      },
      validation: (Rule: Rule) => Rule.required(),
    },
    {
      name: "oppstartsdato",
      title: "Dato for oppstart",
      type: "date",
      options: { dateFormat: "DD/MM/YYYY" },
      hidden: ({ parent }) => parent?.oppstart !== "dato",
    },
    //Faneinnhold
    {
      name: "faneinnhold",
      title: "Faneinnhold",
      type: "faneinnhold",
    },
    {
      name: "kontaktinfoTiltaksansvarlige",
      title: "Tiltaksansvarlig",
      description:
        "Her velger du en eller flere tiltaksansvarlige for tiltaksgjennomføringen",
      type: "array",
      of: [{ type: "reference", to: [{ type: "navKontaktperson" }] }],
      validation: (Rule: Rule) => Rule.required().min(1).unique(),
    },
    {
      name: "lenker",
      title: "Lenker",
      description:
        "Dersom du har lenker som er interessant for tiltaksgjennomføringen kan det legges til her. PS: Per 05.10.2022 er dette feltet ikke synlig for veiledere enda.",
      type: "array",
      of: [lenke],
    },
    {
      name: "tilgjengelighetsstatus",
      title: "Tilgjengelighetsstatus",
      description:
        "Tilgjengelighetsstatus utledes fra data i Arena og kan ikke overskrives her i Sanity.",
      readOnly: true,
      type: "string",
      options: {
        list: [
          { title: "Åpent", value: "Ledig" },
          { title: "Venteliste", value: "Venteliste" },
          { title: "Stengt", value: "Stengt" },
        ],
      },
    },
  ],
  preview: {
    select: {
      title: "tiltaksgjennomforingNavn",
      tiltakstypeNavn: "tiltakstype.tiltakstypeNavn",
      fylke: "fylke.navn",
    },
    prepare: (selection) => {
      const { title, tiltakstypeNavn, fylke } = selection;
      return {
        title,
        subtitle: `${fylke} - ${tiltakstypeNavn}`,
      };
    },
  },
};
