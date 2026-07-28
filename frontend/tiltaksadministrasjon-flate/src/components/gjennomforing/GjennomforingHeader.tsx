import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import {
  isGjennomforingAvtaleDetaljer,
  isGjennomforingEnkeltplassDetaljer,
} from "@/api/gjennomforing/utils";
import { GjennomforingAvtaleHeader } from "@/components/gjennomforing/GjennomforingAvtaleHeader";
import { GjennomforingEnkeltplassHeader } from "@/components/gjennomforing/GjennomforingEnkeltplassHeader";

interface Props {
  gjennomforingId: string;
}

export function GjennomforingHeader({ gjennomforingId }: Props) {
  const detaljer = useGjennomforing(gjennomforingId);

  if (isGjennomforingAvtaleDetaljer(detaljer)) {
    return <GjennomforingAvtaleHeader gjennomforing={detaljer.gjennomforing} />;
  } else if (isGjennomforingEnkeltplassDetaljer(detaljer) && detaljer.deltaker) {
    return (
      <GjennomforingEnkeltplassHeader
        gjennomforing={detaljer.gjennomforing}
        deltaker={detaljer.deltaker}
      />
    );
  } else {
    return null;
  }
}
