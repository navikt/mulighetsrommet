import { SelectOption } from "./SokeSelect";
import { NavAnsatt } from "mulighetsrommet-api-client";

export const AdministratorOptions = (
  ansatt?: NavAnsatt,
  administratorer?: {
    navIdent: string;
    navn: string;
  }[],
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

  if (
    administratorer &&
    !administratorer.map((admin) => admin.navIdent).includes(ansatt?.navIdent)
  ) {
    administratorer.forEach(({ navIdent, navn }) => {
      options.push({
        value: navIdent,
        label: `${navn} - ${navIdent}`,
      });
    });
  }

  betabrukere
    .filter(
      (b: NavAnsatt) =>
        b.navIdent !== ansatt.navIdent &&
        !administratorer?.map((admin) => admin.navIdent).includes(b.navIdent),
    )
    .forEach((b: NavAnsatt) => {
      options.push({
        value: b.navIdent,
        label: `${b.fornavn} ${b.etternavn} - ${b.navIdent}`,
      });
    });
  return options;
};
