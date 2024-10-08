import { GrDocumentPerformance } from "react-icons/gr";
import { defineArrayMember, defineField, defineType, Rule } from "sanity";
import { Information } from "../components/Information";
import { VelgAlleEnheterForKontaktpersoner } from "../components/VelgAlleEnheterForKontaktpersoner";
import { API_VERSION } from "../sanity.config";
import {
  hasDuplicates,
  isTiltakEgenRegi,
  isTiltakEnkeltplassAnskaffet,
  isTiltakGruppe,
  TILTAK_ADMINISTRERES_I_SANITY,
} from "../utils/utils";
import { EnhetType } from "./enhet";

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
      title: "Administratorer",
      type: "array",
      description: "Eiere av innholdet i denne tiltaksgjennomføringen.",
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
      options: {
        filter: "_id in $ider",
        filterParams: { ider: TILTAK_ADMINISTRERES_I_SANITY },
      },
      validation: (rule) =>
        rule.custom((currentValue) => {
          if (currentValue && isTiltakGruppe(currentValue._ref)) {
            return "Gruppetiltak må administreres i admin-flate";
          }
          return currentValue === undefined ? "Tiltakstype er påkrevd" : true;
        }),
    }),
    defineField({
      name: "tiltaksgjennomforingNavn",
      title: "Navn på tiltaksgjennomføring",
      description: "Navnet kommer fra Arena for tiltak i egen regi (AMS/IPS)",
      type: "string",
      validation: (rule) => rule.required(),
      readOnly: ({ document }) => {
        return isTiltakEgenRegi(document.tiltakstype?._ref);
      },
    }),
    defineField({
      name: "tiltaksnummer",
      title: "Tiltaksnummer",
      description: "Tiltaksnummeret kommer fra Arena for tiltak i egen regi (AMS/IPS)",
      type: "slug",
      hidden: ({ document }) => {
        return !(
          isTiltakEgenRegi(document.tiltakstype?._ref) ||
          isTiltakEnkeltplassAnskaffet(document.tiltakstype?._ref)
        );
      },
      readOnly: ({ document }) => {
        return isTiltakEgenRegi(document.tiltakstype?._ref);
      },
    }),
    defineField({
      name: "beskrivelse",
      title: "Beskrivelse",
      description: "Beskrivelse av formålet med tiltaksgjennomføringen.",
      type: "text",
      rows: 5,
      validation: (rule: Rule) => rule.max(500),
    }),
    defineField({
      name: "stedForGjennomforing",
      title: "Sted for gjennomføring",
      description:
        "Skriv inn stedet tiltaket skal gjennomføres, for eksempel Fredrikstad eller Tromsø. For tiltak uten eksplisitt lokasjon kan du la feltet stå tomt.",
      type: "string",
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
      validation: (rule) =>
        rule.custom((currentValue, { document }) => {
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
          if (!document.fylke || !enheter) {
            return true;
          }

          const validEnheter = await getClient({
            apiVersion: API_VERSION,
          }).fetch("*[(_type == 'enhet' && fylke._ref == $fylke) || type == 'Als']._id", {
            fylke: document.fylke._ref,
          });

          const paths = enheter
            ?.filter((enhet) => !validEnheter.includes(enhet._ref))
            ?.map((enhet) => [{ _key: enhet._key }]);

          return !paths.length ? true : { message: "Alle enheter må tilhøre valgt fylke.", paths };
        }),
    }),
    defineField({
      name: "kontaktpersoner",
      title: "Kontaktpersoner",
      description: "Veileders lokale kontaktpersoner for tiltaksgjennomføringen.",
      type: "array",
      of: [
        defineArrayMember({
          type: "object",
          name: "kontaktperson",
          components: { input: VelgAlleEnheterForKontaktpersoner },
          fields: [
            {
              type: "reference",
              name: "navKontaktperson",
              to: [{ type: "navKontaktperson" }],
            },
            {
              type: "array",
              name: "enheter",
              validation: (Rule) =>
                Rule.required().error("Du må velge minst én enhet som kontaktpersonen ekspederer."),
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
            },
            {
              type: "string",
              name: "beskrivelse",
            },
          ],
          preview: {
            select: {
              navn: "navKontaktperson.navn",
              enhet1: "enheter.0.navn",
              enhet2: "enheter.1.navn",
              enhet3: "enheter.2.navn",
              enhet4: "enheter.3.navn",
              enhet5: "enheter.4.navn",
              // Må hardkode fordi det ikke er noen god måte å hente ut alle navn for alle enheter...
            },
            prepare: (data) => {
              const { navn, enhet1, enhet2, enhet3, enhet4, enhet5 } = data;
              return {
                title: navn,
                subtitle: [enhet1, enhet2, enhet3, enhet4, enhet5].filter(Boolean).join(", "),
              };
            },
          },
        }),
      ],
      validation: (rule) =>
        rule.custom((currentValue, { document }) => {
          if (!currentValue || currentValue.length === 0) {
            return "Må ha minst én kontaktperson";
          }
          if (hasDuplicates(currentValue.map((e) => e._key))) {
            return "Innholder duplikater";
          }

          return true;
        }),
    }),
    defineField({
      name: "arrangor",
      title: "Arrangør",
      type: "reference",
      to: [{ type: "arrangor" }],
      hidden: ({ document }) => {
        return !isTiltakEnkeltplassAnskaffet(document.tiltakstype?._ref);
      },
    }),
    defineField({
      name: "faneinnhold",
      title: "Faneinnhold",
      type: "faneinnhold",
    }),
    defineField({
      name: "delingMedBruker",
      title: "Informasjon som kan deles med bruker",
      description:
        "Informasjon om tiltaket som veileder kan dele med bruker. Standard tekst fra tiltakstype brukes hvis ikke utfylt.",
      type: "text",
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
      updatedAt: "_updatedAt",
    },
    prepare: ({ title, tiltaksnummer, updatedAt }) => ({
      title,
      subtitle: `${tiltaksnummer ? tiltaksnummer : ""} - Sist oppd: ${new Date(
        updatedAt,
      ).toLocaleTimeString("no-NO", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
      })}`,
    }),
  },
});
