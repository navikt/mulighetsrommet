import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import { NOM_ANSATT_SIDE } from "@mr/frontend-common/constants";
import { Definisjonsliste } from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";
import { GjennomforingAvtaleDto } from "@tiltaksadministrasjon/api-client";

interface Props {
  gjennomforing: GjennomforingAvtaleDto;
}

export function GjennomforingDetaljerAdministratorer({ gjennomforing }: Props) {
  return (
    <Definisjonsliste
      title="Administratorer"
      definitions={[
        {
          key: gjennomforingTekster.administratorerForGjennomforingenLabel,
          value: gjennomforing.administratorer.length ? (
            <ul>
              {gjennomforing.administratorer.map((admin) => {
                return (
                  <li key={admin.navIdent}>
                    <Lenke to={`${NOM_ANSATT_SIDE}${admin.navIdent}`} isExternal>
                      {`${admin.navn} - ${admin.navIdent}`}{" "}
                    </Lenke>
                  </li>
                );
              })}
            </ul>
          ) : (
            gjennomforingTekster.ingenAdministratorerSattForGjennomforingenLabel
          ),
        },
      ]}
    />
  );
}
