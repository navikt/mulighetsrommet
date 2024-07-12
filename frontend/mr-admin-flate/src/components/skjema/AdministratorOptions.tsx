import { NavAnsatt } from "mulighetsrommet-api-client";
import { SelectOption } from "mulighetsrommet-frontend-common/components/SokeSelect";

export function AdministratorOptions(
  ansatt?: NavAnsatt,
  administratorer?: {
    navIdent: string;
    navn: string;
  }[],
  eksisterendeAdministratorer?: NavAnsatt[],
): SelectOption[] {
  if (!ansatt || !eksisterendeAdministratorer) {
    return [{ value: "", label: "Laster..." }];
  }

  const options = [
    {
      value: ansatt.navIdent ?? "",
      label: `${ansatt.fornavn} ${ansatt?.etternavn} - ${ansatt?.navIdent}`,
    },
  ];

  if (administratorer) {
    administratorer
      .filter((admin) => admin.navIdent !== ansatt?.navIdent)
      .forEach(({ navIdent, navn }) => {
        options.push({
          value: navIdent,
          label: `${navn} - ${navIdent}`,
        });
      });
  }

  eksisterendeAdministratorer
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
}
