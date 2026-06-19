import { useEnkeltplassGjennomforingOrError } from "@/api/gjennomforing/useGjennomforing";
import { Brodsmuler } from "@/components/navigering/Brodsmuler";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { GavelSoundBlockFillIcon } from "@navikt/aksel-icons";
import React from "react";
import { DeltakerHeader } from "../gjennomforing/DeltakerHeader";
import { HeaderBanner } from "@/layouts/HeaderBanner";

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
        ikon={
          <GavelSoundBlockFillIcon
            color="var(--ax-text-brand-blue-decoration)"
            aria-hidden
            width="2.5rem"
            height="2.5rem"
          />
        }
        heading={`Tilskuddsbehandling for ${tiltakstype.navn}`}
      />
      {enkeltplassDeltaker && (
        <DeltakerHeader deltaker={enkeltplassDeltaker} arrangorNavn={gjennomforing.arrangor.navn} />
      )}
      <WhitePaddedBox>{children}</WhitePaddedBox>
    </>
  );
}
