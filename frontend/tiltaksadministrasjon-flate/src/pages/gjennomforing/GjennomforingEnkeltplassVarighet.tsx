import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import { formaterDato } from "@mr/frontend-common/utils/date";
import {
  Definisjonsliste,
  Definition,
} from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";
import { GjennomforingEnkeltplassDto } from "@tiltaksadministrasjon/api-client";

interface Props {
  gjennomforing: GjennomforingEnkeltplassDto;
}

export function GjennomforingEnkeltplassVarighet({ gjennomforing }: Props) {
  const definitions = getVarighetOgPameldingEnkeltplass(gjennomforing);
  return <Definisjonsliste title="Varighet" definitions={definitions} />;
}

function getVarighetOgPameldingEnkeltplass(
  gjennomforing: GjennomforingEnkeltplassDto,
): Definition[] {
  return [
    {
      key: gjennomforingTekster.startdatoLabel,
      value: formaterDato(gjennomforing.startDato),
    },
    {
      key: gjennomforingTekster.sluttdatoLabel,
      value: formaterDato(gjennomforing.sluttDato) ?? "-",
    },
  ];
}
