import { BodyShort, Table } from "@navikt/ds-react";
import {
  ArrangorflateGjennomforingDto,
  ArrangorflateTiltakstypeDto,
} from "@arrangor-utbetalinger/api-client";

interface Props {
  tiltakstype: ArrangorflateTiltakstypeDto;
  gjennomforing: ArrangorflateGjennomforingDto;
}

export function TiltakHeaderCell({ tiltakstype, gjennomforing }: Props) {
  return (
    <Table.HeaderCell scope="row">
      <strong>{tiltakstype.navn}</strong>
      <BodyShort>
        {gjennomforing.navn} ({gjennomforing.lopenummer})
      </BodyShort>
    </Table.HeaderCell>
  );
}
