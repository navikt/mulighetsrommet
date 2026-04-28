import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { useTiltakstype } from "@/api/tiltakstyper/useTiltakstype";
import { isEnkeltplass, isGruppetiltak } from "@/api/gjennomforing/utils";
import { GjennomforingAvtaleDetaljer } from "@/components/gjennomforing/GjennomforingAvtaleDetaljer";
import { GjennomforingEnkeltplassDetaljer } from "@/components/gjennomforing/GjennomforingEnkeltplassDetaljer";

export function GjennomforingDetaljer() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const detaljer = useGjennomforing(gjennomforingId);
  const {
    gjennomforing,
    veilederinfo,
    utdanningslop,
    amoKategorisering,
    prismodell,
    okonomi,
    enkeltplassDeltaker,
  } = detaljer;
  const tiltakstype = useTiltakstype(detaljer.tiltakstype.id);

  if (isGruppetiltak(gjennomforing)) {
    return (
      <GjennomforingAvtaleDetaljer
        tiltakstype={tiltakstype}
        gjennomforing={gjennomforing}
        veilederinfo={veilederinfo}
        prismodell={prismodell}
        amoKategorisering={amoKategorisering}
        utdanningslop={utdanningslop}
      />
    );
  } else if (isEnkeltplass(gjennomforing)) {
    return (
      <GjennomforingEnkeltplassDetaljer
        tiltakstype={tiltakstype}
        gjennomforing={gjennomforing}
        veilederinfo={veilederinfo}
        prismodell={prismodell}
        enkeltplassDeltaker={enkeltplassDeltaker}
        okonomi={okonomi}
      />
    );
  }
}
