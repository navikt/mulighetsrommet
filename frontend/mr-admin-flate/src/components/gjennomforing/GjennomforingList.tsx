import { Alert, BodyShort } from "@navikt/ds-react";
import { Laster } from "../laster/Laster";
import { GjennomforingFilter } from "@/api/atoms";
import { GjennomforingDto } from "@mr/api-client";
import { ReactNode } from "react";
import { GjennomforingStatusTag } from "@mr/frontend-common";
import { useAdminGjennomforinger } from "@/api/gjennomforing/useAdminGjennomforinger";

interface Props {
  filter: Partial<GjennomforingFilter>;
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
            <GjennomforingStatusTag status={gjennomforing.status.status} />
            {props.action(gjennomforing)}
          </li>
        ))}
      </ul>
    </div>
  );
}
