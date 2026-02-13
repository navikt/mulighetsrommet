import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import { formaterPeriodeUdefinertSlutt } from "@mr/frontend-common/utils/date";
import { Link as ReactRouterLink } from "react-router";
import { Definisjonsliste } from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";
import { AvtaleDto } from "@tiltaksadministrasjon/api-client";
import { Link } from "@navikt/ds-react";

interface Props {
  avtale: AvtaleDto;
}

export function GjennomforingDetaljerAvtale({ avtale }: Props) {
  return (
    <Definisjonsliste
      title="Avtaledetaljer"
      definitions={[
        {
          key: gjennomforingTekster.avtaleLabel,
          value: (
            <Link as={ReactRouterLink} to={`/avtaler/${avtale.id}`}>
              {avtale.navn} {avtale.avtalenummer ?? null}
            </Link>
          ),
        },
        {
          key: "Avtaleperiode",
          value: `${formaterPeriodeUdefinertSlutt({ start: avtale.startDato, slutt: avtale.sluttDato })}`,
        },
      ]}
    />
  );
}
