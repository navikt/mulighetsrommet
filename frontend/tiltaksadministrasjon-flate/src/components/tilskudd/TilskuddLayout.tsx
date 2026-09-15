import { useEnkeltplassGjennomforingOrError } from "@/api/gjennomforing/useGjennomforing";
import { Brodsmuler } from "@/components/navigering/Brodsmuler";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import React from "react";
import { GjennomforingEnkeltplassHeader } from "../gjennomforing/GjennomforingEnkeltplassHeader";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { TilskuddIkon } from "@/components/ikoner/TilskuddIkon";

interface Props {
  gjennomforingId: string;
  children: React.ReactNode;
}

export function TilskuddLayout({ gjennomforingId, children }: Props) {
  const { gjennomforing, deltaker, tiltakstype } =
    useEnkeltplassGjennomforingOrError(gjennomforingId);

  return (
    <>
      <title>Tilskudd</title>
      <Brodsmuler
        brodsmuler={[
          { tittel: "Gjennomføringer", lenke: `/gjennomforinger` },
          { tittel: "Gjennomføring", lenke: `/gjennomforinger/${gjennomforingId}` },
          {
            tittel: "Tilskudd",
            lenke: `/gjennomforinger/${gjennomforingId}/tilskudd` as const,
          },
          { tittel: "Tilskudd" },
        ]}
      />
      <HeaderBanner
        ikon={<TilskuddIkon />}
        heading={`Tilskudd for ${tiltakstype.navn}`}
      />
      {deltaker && (
        <GjennomforingEnkeltplassHeader gjennomforing={gjennomforing} deltaker={deltaker} />
      )}
      <WhitePaddedBox>{children}</WhitePaddedBox>
    </>
  );
}
