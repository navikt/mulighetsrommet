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
      type: "blockContent",
      group: "forHvem",
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
      type: "blockContent",
      group: "detaljerOgInnhold",
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
      type: "blockContent",
      group: "pameldingOgVarighet",
    },
  ],
};
