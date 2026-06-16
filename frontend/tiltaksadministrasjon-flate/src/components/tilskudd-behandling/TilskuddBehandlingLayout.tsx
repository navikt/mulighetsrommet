import { useEnkeltplassGjennomforingOrError } from "@/api/gjennomforing/useGjennomforing";
import { Header } from "@/components/detaljside/Header";
import { Brodsmuler } from "@/components/navigering/Brodsmuler";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { DataElementStatusTag } from "@mr/frontend-common";
import { GavelSoundBlockFillIcon } from "@navikt/aksel-icons";
import { Heading } from "@navikt/ds-react";
import { DataElementStatus } from "@tiltaksadministrasjon/api-client";
import React from "react";
import { DeltakerHeader } from "../gjennomforing/DeltakerHeader";

export type TilskuddBehandlingTab = "saksopplysninger" | "vedtak";

interface Props {
  gjennomforingId: string;
  status?: DataElementStatus;
  children: React.ReactNode;
}

export function TilskuddBehandlingLayout({ gjennomforingId, status, children }: Props) {
  const { gjennomforing, enkeltplassDeltaker, tiltakstype } =
    useEnkeltplassGjennomforingOrError(gjennomforingId);

  return (
    <>
      <title>Tilskuddsbehandling</title>
      <Brodsmuler
        brodsmuler={[
          { tittel: "Gjennomføringer", lenke: `/gjennomforinger` },
          { tittel: "Gjennomføring", lenke: `/gjennomforinger/${gjennomforingId}` },
          {
            tittel: "Tilskuddsbehandlinger",
            lenke: `/gjennomforinger/${gjennomforingId}/tilskudd-behandling` as const,
          },
          { tittel: "Behandling" },
        ]}
      />
      <Header>
        <GavelSoundBlockFillIcon
          color="var(--ax-text-brand-blue-decoration)"
          aria-hidden
          width="2.5rem"
          height="2.5rem"
        />
        <Heading size="large" level="2">
          {`Tilskuddsbehandling for ${tiltakstype.navn}`}
        </Heading>
        {status && <DataElementStatusTag {...status} />}
      </Header>
      {enkeltplassDeltaker && (
        <DeltakerHeader deltaker={enkeltplassDeltaker} arrangorNavn={gjennomforing.arrangor.navn} />
      )}
      <WhitePaddedBox>{children}</WhitePaddedBox>
    </>
  );
}
