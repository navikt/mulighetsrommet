import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { useTiltakstype } from "@/api/tiltakstyper/useTiltakstype";
import {
  isGjennomforingAvtaleDetaljer,
  isGjennomforingEnkeltplassDetaljer,
} from "@/api/gjennomforing/utils";
import { GjennomforingAvtaleDetaljer } from "@/components/gjennomforing/GjennomforingAvtaleDetaljer";
import { GjennomforingEnkeltplassDetaljer } from "@/components/gjennomforing/GjennomforingEnkeltplassDetaljer";

export function GjennomforingDetaljer() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const detaljer = useGjennomforing(gjennomforingId);
  const tiltakstype = useTiltakstype(detaljer.tiltakstype.id);

  if (isGjennomforingAvtaleDetaljer(detaljer)) {
    return (
      <GjennomforingAvtaleDetaljer
        tiltakstype={tiltakstype}
        gjennomforing={detaljer.gjennomforing}
        veilederinfo={detaljer.veilederinfo}
        prismodell={detaljer.prismodell}
        opplaring={detaljer.opplaring}
      />
    );
  } else if (isGjennomforingEnkeltplassDetaljer(detaljer)) {
    return (
      <GjennomforingEnkeltplassDetaljer
        tiltakstype={tiltakstype}
        gjennomforing={detaljer.gjennomforing}
        prismodell={detaljer.prismodell}
        deltaker={detaljer.deltaker}
        okonomi={detaljer.okonomi}
        prisendring={detaljer.prisendring}
        opplaring={detaljer.opplaring}
      />
    );
  }
}
