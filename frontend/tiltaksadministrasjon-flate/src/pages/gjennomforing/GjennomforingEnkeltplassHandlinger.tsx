import { Endringshistorikk } from "@/components/endringshistorikk/Endringshistorikk";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import {
  EndringshistorikkType,
  GjennomforingEnkeltplassDto,
} from "@tiltaksadministrasjon/api-client";

interface Props {
  gjennomforing: GjennomforingEnkeltplassDto;
}

export function GjennomforingEnkeltplassHandlinger({ gjennomforing }: Props) {
  return (
    <KnapperadContainer>
      <Endringshistorikk id={gjennomforing.id} type={EndringshistorikkType.GJENNOMFORING} />
    </KnapperadContainer>
  );
}
