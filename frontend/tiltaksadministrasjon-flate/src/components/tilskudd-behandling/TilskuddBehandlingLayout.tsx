import { useEnkeltplassGjennomforingOrError } from "@/api/gjennomforing/useGjennomforing";
import { Brodsmuler } from "@/components/navigering/Brodsmuler";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import React from "react";
import { GjennomforingEnkeltplassHeader } from "../gjennomforing/GjennomforingEnkeltplassHeader";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { TilskuddIkon } from "@/components/ikoner/TilskuddIkon";

export type TilskuddBehandlingTab = "saksopplysninger" | "vedtak";

interface Props {
  gjennomforingId: string;
  children: React.ReactNode;
}

export function TilskuddBehandlingLayout({ gjennomforingId, children }: Props) {
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
      <HeaderBanner
        ikon={<TilskuddIkon />}
        heading={`Tilskuddsbehandling for ${tiltakstype.navn}`}
      />
      {enkeltplassDeltaker && (
        <GjennomforingEnkeltplassHeader
          gjennomforing={gjennomforing}
          deltaker={enkeltplassDeltaker}
        />
      )}
      <WhitePaddedBox>{children}</WhitePaddedBox>
    </>
  );
}
