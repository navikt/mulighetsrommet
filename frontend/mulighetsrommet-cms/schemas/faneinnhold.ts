import { blockContentValidation } from "../validation/blockContentValidation";
import { defineField, defineType } from "sanity";

const MAKS_LENGDE_INNHOLD = 2500;
const infoboksOptions = {
  type: "text",
  validation: (Rule) => Rule.max(300),
  rows: 3,
};

export const faneinnhold = defineType({
  name: "faneinnhold",
  title: "Faneinnhold",
  type: "object",
  groups: [
    {
      name: "forHvem",
      title: "For hvem",
    },
    {
      name: "detaljerOgInnhold",
      title: "Detaljer og innhold",
    },
    {
      name: "pameldingOgVarighet",
      title: "Påmelding og varighet",
    },
    {
      name: "kontaktinfo",
      title: "Kontaktinfo",
    },
    {
      name: "lenker",
      title: "Lenker",
    },
  ],
  fields: [
    defineField({
      name: "forHvemInfoboks",
      title:
        'Fremhevet informasjon til veileder som legger seg i blå infoboks under fanen "For hvem"',
      description:
        'Bruk denne tekstboksen for informasjon som skal være ekstra fremtredende for veilederne. Bruk gjerne "forhåndsvisning av tiltaksgjennomføring" for å få et inntrykk av hvordan det vil se ut.',
      ...infoboksOptions,
      group: "forHvem",
    }),
    defineField({
      name: "forHvem",
      title: "For hvem",
      description: `Beskrivelse av hvem tiltakstypen passer for. Husk å bruke et kort og konsist språk. Makslengde er ${MAKS_LENGDE_INNHOLD} tegn.`,
      type: "blockContent",
      group: "forHvem",
      validation: (Rule) =>
        Rule.custom((doc) =>
          blockContentValidation(
            doc,
            MAKS_LENGDE_INNHOLD,
            "Innholdet er for langt. Kan innholdet være kortere og mer konsist?",
          ),
        ),
    }),
    defineField({
      name: "detaljerOgInnholdInfoboks",
      title:
        'Ekstra viktig informasjon til veileder som legger seg i blå infoboks under fanen "Detaljer og innhold"',
      description:
        'Fremhevet informasjon som skal være ekstra fremtredende for veilederne. Bruk gjerne "forhåndsvisning av tiltaksgjennomføring" for å få et inntrykk av hvordan det vil se ut.',
      ...infoboksOptions,
      group: "detaljerOgInnhold",
    }),
    defineField({
      name: "detaljerOgInnhold",
      title: "Detaljer og innhold",
      description: `Beskrivelse av detaljer og innhold for tiltaktypen. Husk å bruke et kort og konsist språk. Makslengde er ${MAKS_LENGDE_INNHOLD} tegn.`,
      type: "blockContent",
      group: "detaljerOgInnhold",
      validation: (Rule) =>
        Rule.custom((doc) =>
          blockContentValidation(
            doc,
            MAKS_LENGDE_INNHOLD,
            "Innholdet er for langt. Kan innholdet være kortere og mer konsist?",
          ),
        ),
    }),
    defineField({
      name: "pameldingOgVarighetInfoboks",
      title:
        'Ekstra viktig informasjon til veileder som legger seg i blå infoboks under fanen "Påmelding og varighet"',
      description:
        'Fremhevet informasjon om påmelding og varighet. Bruk gjerne "forhåndsvisning av tiltaksgjennomføring" for å få et inntrykk av hvordan det vil se ut.',
      ...infoboksOptions,
      group: "pameldingOgVarighet",
    }),
    defineField({
      name: "pameldingOgVarighet",
      title: "Påmelding og varighet",
      description: `Beskrivelse av rutiner rundt påmelding og varighet i tiltaket. Husk å bruke et kort og konsist språk. Makslengde er ${MAKS_LENGDE_INNHOLD} tegn.`,
      type: "blockContent",
      group: "pameldingOgVarighet",
      validation: (Rule) =>
        Rule.custom((doc) =>
          blockContentValidation(
            doc,
            MAKS_LENGDE_INNHOLD,
            "Innholdet er for langt. Kan innholdet være kortere og mer konsist?",
          ),
        ),
    }),
    defineField({
      name: "kontaktinfoInfoboks",
      title:
        'Ekstra viktig informasjon til veileder som legger seg i blå infoboks under fanen "Kontaktinfo"',
      description:
        'Fremhevet informasjon om kontaktinfo. Bruk gjerne "forhåndsvisning av tiltaksgjennomføring" for å få et inntrykk av hvordan det vil se ut.',
      ...infoboksOptions,
      group: "kontaktinfo",
    }),
    defineField({
      name: "kontaktinfo",
      title: "Kontaktinfo",
      description: `Ekstra tekst om kontaktinfo. Makslengde er ${MAKS_LENGDE_INNHOLD} tegn.`,
      type: "blockContent",
      group: "kontaktinfo",
      validation: (Rule) =>
        Rule.custom((doc) =>
          blockContentValidation(
            doc,
            MAKS_LENGDE_INNHOLD,
            "Innholdet er for langt. Kan innholdet være kortere og mer konsist?",
          ),
        ),
    }),
    defineField({
      name: "lenker",
      title: "Lenker",
      description: "KOMMER SNART - Legg til lenker som blir synlig for NAV-ansatte",
      type: "array",
      of: [{ type: "lenke" }],
      readOnly: false,
    }),
  ],
});
