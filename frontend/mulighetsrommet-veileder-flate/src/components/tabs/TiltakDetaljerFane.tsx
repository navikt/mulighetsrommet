import { PortableTextTypedObject } from "@api-client";
import { LokalInformasjonContainer } from "@mr/frontend-common";
import { Alert, BodyLong, Heading } from "@navikt/ds-react";
import { RedaksjoneltInnhold } from "../RedaksjoneltInnhold";
import { TiltakDetaljerFaneContainer } from "./TiltakDetaljerFaneContainer";

interface DetaljerFaneProps {
  gjennomforingAlert?: string | null;
  tiltakstypeAlert?: string | null;
  gjennomforing?: PortableTextTypedObject[] | null;
  tiltakstype?: PortableTextTypedObject[] | null;
}

export function TiltakDetaljerFane({
  gjennomforingAlert,
  tiltakstypeAlert,
  gjennomforing,
  tiltakstype,
}: DetaljerFaneProps) {
  return (
    <TiltakDetaljerFaneContainer
      className="flex flex-col gap-2"
      harInnhold={!!gjennomforingAlert || !!tiltakstypeAlert || !!gjennomforing || !!tiltakstype}
    >
      {tiltakstypeAlert && (
        <Alert variant="info" className="whitespace-pre-wrap">
          {tiltakstypeAlert}
        </Alert>
      )}
      <BodyLong as="div" size="large" className="prose">
        <RedaksjoneltInnhold value={tiltakstype} />
      </BodyLong>
      {(gjennomforing || gjennomforingAlert) && (
        <LokalInformasjonContainer>
          <Heading level="2" size="small">
            Lokal informasjon
          </Heading>
          {gjennomforingAlert && (
            <Alert variant="info" className="my-4 whitespace-pre-wrap">
              {gjennomforingAlert}
            </Alert>
          )}
          <RedaksjoneltInnhold value={gjennomforing} />
        </LokalInformasjonContainer>
      )}
    </TiltakDetaljerFaneContainer>
  );
}
