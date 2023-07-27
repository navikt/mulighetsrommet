import { SelectOption } from "./SokeSelect";
import { NavAnsatt } from "mulighetsrommet-api-client";

export const AnsvarligOptions = (
  ansatt?: NavAnsatt,
  ansvarlig?: { navident: string; navn: string },
  betabrukere?: NavAnsatt[],
): SelectOption[] => {
  if (!ansatt || !betabrukere) {
    return [{ value: "", label: "Laster..." }];
  }

  const options = [
    {
      value: ansatt.navIdent ?? "",
      label: `${ansatt.fornavn} ${ansatt?.etternavn} - ${ansatt?.navIdent}`,
    },
  ];

  if (ansvarlig?.navident && ansvarlig.navident !== ansatt?.navIdent) {
    options.push({
      value: ansvarlig.navident,
      label: `${ansvarlig.navn} - ${ansvarlig.navident}`,
    });
  }

  betabrukere
    .filter(
      (b: NavAnsatt) =>
        b.navIdent !== ansatt.navIdent && b.navIdent !== ansvarlig?.navident,
    )
    .forEach((b: NavAnsatt) =>
      options.push({
        value: b.navIdent,
        label: `${b.fornavn} ${b.etternavn} - ${b.navIdent}`,
      }),
    );

  return options;
};
