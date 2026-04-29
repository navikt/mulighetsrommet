import { useEnkeltplassGjennomforingOrError } from "@/api/gjennomforing/useGjennomforing";
import { Header } from "@/components/detaljside/Header";
import { GjennomforingDetaljerMini } from "@/components/gjennomforing/GjennomforingDetaljerMini";
import { Brodsmuler } from "@/components/navigering/Brodsmuler";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { GavelSoundBlockFillIcon } from "@navikt/aksel-icons";
import { Heading } from "@navikt/ds-react";
import React from "react";

export type TilskuddBehandlingTab = "saksopplysninger" | "vedtak";

interface Props {
  gjennomforingId: string;
  children: React.ReactNode;
}

export function TilskuddBehandlingLayout({ gjennomforingId, children }: Props) {
  const { gjennomforing } = useEnkeltplassGjennomforingOrError(gjennomforingId);

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
          Tilskuddsbehandling
        </Heading>
      </Header>
      <WhitePaddedBox>
        <GjennomforingDetaljerMini gjennomforing={gjennomforing} />
        {children}
      </WhitePaddedBox>
    </>
  );
}
