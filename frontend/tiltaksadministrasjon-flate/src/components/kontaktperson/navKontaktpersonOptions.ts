import type { ComboboxOption } from "@/components/skjema/FormCombobox";

export interface NavAnsattSokTreff {
  navIdent: string;
  fornavn: string;
  etternavn: string;
}

export function formaterNavn(treff: NavAnsattSokTreff) {
  return `${treff.fornavn} ${treff.etternavn}`;
}

export function getNavKontaktpersonOptions(args: {
  valgtNavIdent: string | null | undefined;
  alleValgteNavIdenter: (string | null | undefined)[];
  sokeresultat: NavAnsattSokTreff[] | undefined;
  kjenteNavn: Record<string, string>;
}): ComboboxOption[] {
  const { valgtNavIdent, alleValgteNavIdenter, sokeresultat, kjenteNavn } = args;
  const valgteIdenter = new Set(alleValgteNavIdenter);
  const valgtTreff = valgtNavIdent
    ? sokeresultat?.find((treff) => treff.navIdent === valgtNavIdent)
    : undefined;
  const navn = valgtTreff
    ? formaterNavn(valgtTreff)
    : valgtNavIdent
      ? kjenteNavn[valgtNavIdent]
      : undefined;
  const valgteAlternativer: ComboboxOption[] = valgtNavIdent
    ? [
        {
          label: navn ? `${navn} - ${valgtNavIdent}` : valgtNavIdent,
          value: valgtNavIdent,
        },
      ]
    : [];

  const sokAlternativer =
    sokeresultat
      ?.filter((treff) => !valgteIdenter.has(treff.navIdent))
      .map((treff) => ({
        label: `${formaterNavn(treff)} - ${treff.navIdent}`,
        value: treff.navIdent,
      })) ?? [];

  return [...valgteAlternativer, ...sokAlternativer];
}
