import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import { formatertVentetid } from "@/utils/Utils";
import { formaterDato } from "@mr/frontend-common/utils/date";
import {
  Definisjonsliste,
  Definition,
} from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";
import { kreverDeltidsprosent } from "@/utils/tiltakstype";
import { isGruppetiltak } from "@/api/gjennomforing/utils";
import {
  GjennomforingDto,
  GjennomforingEnkeltplassDto,
  GjennomforingAvtaleDto,
  GjennomforingVeilederinfoDto,
  TiltakstypeDto,
} from "@tiltaksadministrasjon/api-client";

interface Props {
  tiltakstype: TiltakstypeDto;
  gjennomforing: GjennomforingDto;
  veilederinfo: GjennomforingVeilederinfoDto | null;
}

export function GjennomforingDetaljerVarighet({ tiltakstype, gjennomforing, veilederinfo }: Props) {
  const varighetMeta = isGruppetiltak(gjennomforing)
    ? getVarighetOgPameldingGruppe(tiltakstype, gjennomforing, veilederinfo)
    : getVarighetOgPameldingEnkeltplass(gjennomforing);

  return <Definisjonsliste title="Varighet og påmelding" definitions={varighetMeta} />;
}

function getVarighetOgPameldingGruppe(
  tiltakstype: TiltakstypeDto,
  gjennomforing: GjennomforingAvtaleDto,
  veilederinfo: GjennomforingVeilederinfoDto | null,
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
      key: gjennomforingTekster.oppstart.label,
      value: gjennomforingTekster.oppstart.beskrivelse(gjennomforing.oppstart),
    },
    {
      key: gjennomforingTekster.apentForPameldingLabel,
      value: gjennomforing.apentForPamelding ? "Ja" : "Nei",
    },
    {
      key: gjennomforingTekster.antallPlasserLabel,
      value: gjennomforing.antallPlasser,
    },
    kreverDeltidsprosent(tiltakstype) && {
      key: gjennomforingTekster.deltidsprosentLabel,
      value: gjennomforing.deltidsprosent,
    },
    veilederinfo?.estimertVentetid && {
      key: gjennomforingTekster.estimertVentetidLabel,
      value: formatertVentetid(
        veilederinfo.estimertVentetid.verdi,
        veilederinfo.estimertVentetid.enhet,
      ),
    },
    {
      key: gjennomforingTekster.pamelding.label,
      value: gjennomforingTekster.pamelding.beskrivelse(gjennomforing.pameldingType),
    },
  ].filter((definition) => !!definition);
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
