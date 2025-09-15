import { SelectOption } from "@mr/frontend-common/components/SokeSelect";
import { NavAnsattDto } from "@tiltaksadministrasjon/api-client";

export function AdministratorOptions(
  ansatt: NavAnsattDto,
  administratorer: string[],
  eksisterendeAdministratorer: NavAnsattDto[],
): SelectOption[] {
  const adminMap = new Map(eksisterendeAdministratorer.map((a) => [a.navIdent, a]));

  const options = [
    {
      value: ansatt.navIdent,
      label: `${ansatt.fornavn} ${ansatt.etternavn} - ${ansatt.navIdent}`,
    },
  ];

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

  eksisterendeAdministratorer
    .filter((b) => b.navIdent !== ansatt.navIdent && !administratorer.includes(b.navIdent))
    .forEach((b) => {
      options.push({
        value: b.navIdent,
        label: `${b.fornavn} ${b.etternavn} - ${b.navIdent}`,
      });
    });
  return options;
}
