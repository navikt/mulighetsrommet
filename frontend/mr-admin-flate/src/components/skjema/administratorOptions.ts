import { SelectOption } from "@mr/frontend-common/components/SokeSelect";
import { NavAnsattDto } from "@tiltaksadministrasjon/api-client";

export function administratorOptions(navAnsatte: NavAnsattDto[]): SelectOption[] {
  return navAnsatte.map((b) => ({
    value: b.navIdent,
    label: `${b.fornavn} ${b.etternavn} - ${b.navIdent}`,
  }));
}
