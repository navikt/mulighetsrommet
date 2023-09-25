import { SelectOption } from "./SokeSelect";
import { NavAnsatt } from "mulighetsrommet-api-client";

export const AdministratorOptions = (
  ansatt?: NavAnsatt,
  administrator?: {
    navIdent: string;
    navn: string;
  },
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

  if (administrator?.navIdent && administrator.navIdent !== ansatt?.navIdent) {
    options.push({
      value: administrator.navIdent,
      label: `${administrator.navn} - ${administrator.navIdent}`,
    });
  }

  betabrukere
    .filter(
      (b: NavAnsatt) => b.navIdent !== ansatt.navIdent && b.navIdent !== administrator?.navIdent,
    )
    .forEach((b: NavAnsatt) =>
      options.push({
        value: b.navIdent,
        label: `${b.fornavn} ${b.etternavn} - ${b.navIdent}`,
      }),
    );

  return options;
};
