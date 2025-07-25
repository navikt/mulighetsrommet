import { NavAnsatt } from "@mr/api-client-v2";
import { SelectOption } from "@mr/frontend-common/components/SokeSelect";

export function AdministratorOptions(
  ansatt: NavAnsatt,
  administratorer?: string[],
  eksisterendeAdministratorer?: NavAnsatt[],
): SelectOption[] {
  if (!ansatt || !eksisterendeAdministratorer) {
    return [{ value: "", label: "Laster..." }];
  }
  const adminMap = new Map(eksisterendeAdministratorer.map((a) => [a.navIdent, a]));

  const options = [
    {
      value: ansatt.navIdent,
      label: `${ansatt.fornavn} ${ansatt.etternavn} - ${ansatt.navIdent}`,
    },
  ];

  if (administratorer) {
    administratorer
      .filter((ident) => ident !== ansatt.navIdent)
      .forEach((ident) => {
        const match = adminMap.get(ident);
        if (match) {
          options.push({
            value: ident,
            label: `${match.fornavn} ${match.etternavn} - ${ident}`,
          });
        }
      });
  }

  eksisterendeAdministratorer
    .filter(
      (b: NavAnsatt) => b.navIdent !== ansatt.navIdent && !administratorer?.includes(b.navIdent),
    )
    .forEach((b: NavAnsatt) => {
      options.push({
        value: b.navIdent,
        label: `${b.fornavn} ${b.etternavn} - ${b.navIdent}`,
      });
    });
  return options;
}
