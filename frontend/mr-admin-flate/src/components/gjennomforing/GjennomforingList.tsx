import { Alert, BodyShort } from "@navikt/ds-react";
import { Laster } from "../laster/Laster";
import { GjennomforingDto } from "@mr/api-client-v2";
import { ReactNode } from "react";
import { useAdminGjennomforinger } from "@/api/gjennomforing/useAdminGjennomforinger";
import { GjennomforingFilterType } from "@/pages/gjennomforing/filter";
import { GjennomforingStatusTag } from "@/components/statuselementer/GjennomforingStatusTag";

interface Props {
  filter: Partial<GjennomforingFilterType>;
  action: (gjennomforing: GjennomforingDto) => ReactNode;
}

export function GjennomforingList(props: Props) {
  const { data, isError, isPending } = useAdminGjennomforinger(props.filter);

  if (isError) {
    return <Alert variant="error">Vi hadde problemer med henting av tiltaksgjennomføringer</Alert>;
  }

  if (isPending) {
    return <Laster size="xlarge" tekst="Laster tiltaksgjennomføringer..." />;
  }

  const gjennomforinger = data.data;

  return (
    <div>
      <div className="grid grid-cols-[2fr_1fr_1fr_1fr] border-b border-border-divider p-4 items-center gap-2">
        <BodyShort className="font-bold">Tittel</BodyShort>
        <BodyShort className="font-bold">Tiltaksnr.</BodyShort>
        <BodyShort className="font-bold">Status</BodyShort>
      </div>

      <ul className="overflow-y-auto list-none m-0 p-0 max-h-[30rem]">
        {gjennomforinger.map((gjennomforing) => (
          <li
            key={gjennomforing.id}
            className="grid grid-cols-[2fr_1fr_1fr_1fr] border-b border-border-divider p-4 items-center gap-2"
          >
            <BodyShort>{gjennomforing.navn}</BodyShort>
            <BodyShort>{gjennomforing.tiltaksnummer}</BodyShort>
            <GjennomforingStatusTag status={gjennomforing.status} />
            {props.action(gjennomforing)}
          </li>
        ))}
      </ul>
    </div>
  );
}
