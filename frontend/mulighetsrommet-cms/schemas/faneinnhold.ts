import { blockContentValidation } from "../validation/blockContentValidation";
import { defineField, defineType, SanityDocument } from "sanity";

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
  ],
  fields: [
    defineField({
      name: "forHvemInfoboks",
      title:
        'Ekstra viktig informasjon til veileder som legger seg i blå infoboks under fanen "For hvem"',
      description:
        'Bruk denne tekstboksen for informasjon som skal være ekstra fremtredende for veilederne. Bruk gjerne "forhåndsvisning av tiltaksgjennomføring for å få et inntrykk av hvordan det vil se ut."',
      ...infoboksOptions,
      group: "forHvem",
    }),
    defineField({
      name: "forHvem",
      title: "For hvem",
      description: `Her kan du skrive en beskrivelse av hvem tiltakstypen gjelder for. Husk å bruke et kort og konsist språk. Makslengde er ${MAKS_LENGDE_INNHOLD} tegn.`,
      type: "blockContent",
      group: "forHvem",
      validation: (Rule) =>
        Rule.custom((doc) =>
          blockContentValidation(
            doc,
            MAKS_LENGDE_INNHOLD,
            "Innholdet er for langt. Kan innholdet være kortere og mer konsist?"
          )
        ),
    }),
    defineField({
      name: "detaljerOgInnholdInfoboks",
      title:
        'Ekstra viktig informasjon til veileder som legger seg i blå infoboks under fanen "Detaljer og innhold"',
      description:
        'Bruk denne tekstboksen for informasjon som skal være ekstra fremtredende for veilederne. Bruk gjerne "forhåndsvisning av tiltaksgjennomføring for å få et inntrykk av hvordan det vil se ut."',
      ...infoboksOptions,
      group: "detaljerOgInnhold",
    }),
    defineField({
      name: "detaljerOgInnhold",
      title: "Detaljer og innhold",
      description: `Her kan du skrive om detaljer og innhold som gjelder for tiltakstypen. Husk å bruke et kort og konsist språk. Makslengde er ${MAKS_LENGDE_INNHOLD} tegn.`,
      type: "blockContent",
      group: "detaljerOgInnhold",
      validation: (Rule) =>
        Rule.custom((doc) =>
          blockContentValidation(
            doc,
            MAKS_LENGDE_INNHOLD,
            "Innholdet er for langt. Kan innholdet være kortere og mer konsist?"
          )
        ),
    }),
    defineField({
      name: "pameldingOgVarighetInfoboks",
      title:
        'Ekstra viktig informasjon til veileder som legger seg i blå infoboks under fanen "Påmelding og varighet"',
      description:
        'Bruk denne tekstboksen for informasjon som skal være ekstra fremtredende for veilederne. Bruk gjerne "forhåndsvisning av tiltaksgjennomføring for å få et inntrykk av hvordan det vil se ut."',
      ...infoboksOptions,
      group: "pameldingOgVarighet",
    }),
    defineField({
      name: "pameldingOgVarighet",
      title: "Påmelding og varighet",
      description: `Her kan du skrive om påmelding og varighet for tiltakstypen. Husk å bruke et kort og konsist språk. Makslengde er ${MAKS_LENGDE_INNHOLD} tegn.`,
      type: "blockContent",
      group: "pameldingOgVarighet",
      validation: (Rule) =>
        Rule.custom((doc) =>
          blockContentValidation(
            doc,
            MAKS_LENGDE_INNHOLD,
            "Innholdet er for langt. Kan innholdet være kortere og mer konsist?"
          )
        ),
    }),
  ],
});
