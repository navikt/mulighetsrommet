import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { isGruppetiltak } from "@/api/gjennomforing/utils";
import { GjennomforingAvtaleHeader } from "@/components/gjennomforing/GjennomforingAvtaleHeader";
import { GjennomforingEnkeltplassHeader } from "@/components/gjennomforing/GjennomforingEnkeltplassHeader";

interface Props {
  gjennomforingId: string;
}

export function GjennomforingHeader({ gjennomforingId }: Props) {
  const { gjennomforing, enkeltplassDeltaker } = useGjennomforing(gjennomforingId);

  if (isGruppetiltak(gjennomforing)) {
    return <GjennomforingAvtaleHeader gjennomforing={gjennomforing} />;
  } else if (enkeltplassDeltaker) {
    return (
      <GjennomforingEnkeltplassHeader
        gjennomforing={gjennomforing}
        deltaker={enkeltplassDeltaker}
      />
    );
  } else {
    return null;
  }
}
