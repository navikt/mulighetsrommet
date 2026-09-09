import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import { formaterDato } from "@mr/frontend-common/utils/date";
import {
  Definisjonsliste,
  Definition,
} from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";
import { DeltakerDto, GjennomforingEnkeltplassDto } from "@tiltaksadministrasjon/api-client";

interface Props {
  gjennomforing: GjennomforingEnkeltplassDto;
  deltaker: DeltakerDto | null;
}

export function GjennomforingEnkeltplassVarighet({ gjennomforing, deltaker }: Props) {
  const definitions = getVarighetOgPameldingEnkeltplass(gjennomforing, deltaker);
  return <Definisjonsliste title="Varighet" definitions={definitions} />;
}

function getVarighetOgPameldingEnkeltplass(
  gjennomforing: GjennomforingEnkeltplassDto,
  deltaker: DeltakerDto | null,
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
    {
      key: gjennomforingTekster.enkeltplass.deltakelsesmengde.dagerPerUke.label,
      value: deltaker?.dagerPerUke,
    },
  ];
}
