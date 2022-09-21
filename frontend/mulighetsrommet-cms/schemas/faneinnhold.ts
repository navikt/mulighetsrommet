import { blockContentValidation } from "../validation/blockContentValidation";

const MAKS_LENGDE_INNHOLD = 1500;

export default {
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
    {
      name: "forHvemInfoboks",
      title: "For hvem - infoboks",
      description:
        "Hvis denne har innhold, vises det i en infoboks i fanen 'For hvem'",
      type: "string",
      group: "forHvem",
    },
    {
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
    },
    {
      name: "detaljerOgInnholdInfoboks",
      title: "Detaljer og innhold - infoboks",
      description:
        "Hvis denne har innhold, vises det i en infoboks i fanen 'Detaljer og innhold'",
      type: "string",
      group: "detaljerOgInnhold",
    },
    {
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
    },
    {
      name: "pameldingOgVarighetInfoboks",
      title: "Påmelding og varighet - infoboks",
      description:
        "Hvis denne har innhold, vises det i en infoboks i fanen 'Påmelding og varighet'",
      type: "string",
      group: "pameldingOgVarighet",
    },
    {
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
    },
  ],
};
