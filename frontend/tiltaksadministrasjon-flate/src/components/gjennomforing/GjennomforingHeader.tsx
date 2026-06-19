import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { isGruppetiltak } from "@/api/gjennomforing/utils";
import { GjennomforingAvtaleHeader } from "@/components/gjennomforing/GjennomforingAvtaleHeader";
import { DeltakerHeader } from "@/components/gjennomforing/DeltakerHeader";

interface Props {
  gjennomforingId: string;
}

export function GjennomforingHeader({ gjennomforingId }: Props) {
  const { gjennomforing, enkeltplassDeltaker } = useGjennomforing(gjennomforingId);

  if (isGruppetiltak(gjennomforing)) {
    return <GjennomforingAvtaleHeader gjennomforing={gjennomforing} />;
  } else if (enkeltplassDeltaker) {
    return (
      <DeltakerHeader deltaker={enkeltplassDeltaker} arrangorNavn={gjennomforing.arrangor.navn} />
    );
  } else {
    return null;
  }
}
