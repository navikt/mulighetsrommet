import { GrDocumentPerformance } from "react-icons/gr";
import {
  ConditionalPropertyCallbackContext,
  Rule,
  defineField,
  defineType,
} from "sanity";
import { Information } from "../components/Information";
import { ShowFieldIfTiltakstypeMatches } from "../components/ShowFieldIfTiltakstypeMatches";
import { API_VERSION } from "../sanity.config";
import { EnhetType } from "./enhet";
import { hasDuplicates, isIndividueltTiltak } from "../utils/utils";

function erIkkeAdmin(props: ConditionalPropertyCallbackContext): boolean {
  return (
    props.currentUser.roles.find((role) => role.name === "administrator") ===
    undefined
  );
}

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
      description: "Navnet kommer fra Arena/admin-flate",
      type: "string",
      validation: (rule) => rule.required(),
      readOnly: erIkkeAdmin,
    }),
    defineField({
      name: "aar",
      title: "År",
      description:
        "Hvis tiltakstypen gjelder individuelle tiltak skal du ikke fylle inn år.",
      type: "number",
      hidden: true,
      initialValue: () => new Date().getFullYear(),
      readOnly: erIkkeAdmin,
    }),
    defineField({
      name: "lopenummer",
      title: "Løpenummer",
      description:
        "Hvis tiltakstypen gjelder individuelle tiltak skal du ikke fylle inn løpenummer.",
      type: "number",
      hidden: true,
      readOnly: erIkkeAdmin,
    }),
    defineField({
      name: "tiltaksnummer",
      title: "Tiltaksnummer",
      description:
        "Tiltaksnummeret er hentet fra Arena",
      type: "slug",
      hidden: ({document} ) => {
        return isIndividueltTiltak(document.tiltakstype?._ref);
      },
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
      readOnly: erIkkeAdmin,
    }),
    defineField({
      name: "beskrivelse",
      title: "Beskrivelse",
      description: "Beskrivelse av formålet med tiltaksgjennomføringen.",
      type: "text",
      rows: 5,
      components: {
        field: (props) =>
          ShowFieldIfTiltakstypeMatches(props, "Opplæring - Gruppe AMO"), // Viser feltet hvis det er Gruppe AMO som er valgt som tiltakstype
      },
      validation: (rule: Rule) => rule.max(500),
    }),

    defineField({
      name: "tilgjengelighetsstatus",
      title: "Tilgjengelighetsstatus",
      description:
        "Tilgjengelighetsstatus utledes fra data i Arena og kan ikke overskrives i Sanity.",
      readOnly: true,
      type: "string",
      hidden: true,
      options: {
        list: [
          { title: "Åpent", value: "Ledig" },
          { title: "Venteliste", value: "Venteliste" },
          { title: "Stengt", value: "Stengt" },
        ],
      },
    }),
    defineField({
      name: "lokasjon",
      title: "Lokasjon",
      description:
        "Sted for gjennomføring, f.eks. Fredrikstad eller Tromsø. Veileder kan filtrere på verdiene i dette feltet, så ikke skriv fulle adresser.",
      type: "string",
      hidden: ({document} ) => {
        return !isIndividueltTiltak(document.tiltakstype?._ref);
      },
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
      hidden: ({document} ) => {
        return !isIndividueltTiltak(document.tiltakstype?._ref);
      },
      validation: (rule) =>
        rule.custom((currentValue, { document }) => {
          if (!isIndividueltTiltak(document.tiltakstype?._ref)) {
            return true;
          }
          return currentValue === undefined ? "Fylke er påkrevd" : true;
        }),
    }),
    defineField({
      name: "enheter",
      title: "Enheter",
      description:
        "Hvilke enheter kan benytte seg av dette tiltaket? Hvis det gjelder for hele regionen kan dette feltet stå tomt.",
      type: "array",
      hidden: ({ document }) => {
        return !document.fylke || !isIndividueltTiltak(document.tiltakstype?._ref);
      },
      of: [
        {
          type: "reference",
          to: [{ type: "enhet" }],
          options: {
            disableNew: true,
            filter: ({ document }) => {
              return {
                filter: `fylke._ref == $fylke || type == 'Als'`,
                params: {
                  fylke: document.fylke._ref,
                },
              };
            },
          },
        },
      ],
      validation: (rule) =>
        rule.custom(async (enheter, { document, getClient }) => {
          if (!document.fylke || !enheter || !isIndividueltTiltak(document.tiltakstype?._ref)) {
            return true;
          }

          const validEnheter = await getClient({
            apiVersion: API_VERSION,
          }).fetch(
            "*[(_type == 'enhet' && fylke._ref == $fylke) || type == 'Als']._id",
            {
              fylke: document.fylke._ref,
            }
          );

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
      hidden: ({document} ) => {
        return !isIndividueltTiltak(document.tiltakstype?._ref);
      },
      validation: (rule) =>
        rule.custom((currentValue, { document }) => {
          if (!isIndividueltTiltak(document.tiltakstype?._ref)) {
            return true;
          }
          if (!currentValue || currentValue.length === 0) {
            return "Må ha minst én tiltaksansvarlig";
          }
          if (hasDuplicates(currentValue.map(e => e.key))) {
            return "Innholder duplikater";
          }

          return true;
        }),
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
