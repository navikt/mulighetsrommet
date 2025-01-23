import { Tag } from "@navikt/ds-react";
import { TiltakstypeDto, TiltakstypeStatus } from "@mr/api-client-v2";

interface Props {
  tiltakstype: TiltakstypeDto;
}

export function TiltakstypestatusTag({ tiltakstype }: Props) {
  const status = tiltakstype.status;

  function variantAndName(): { variant: "success" | "neutral"; name: string } {
    switch (status) {
      case TiltakstypeStatus.AKTIV:
        return { variant: "success", name: "Aktiv" };
      case TiltakstypeStatus.AVSLUTTET:
        return { variant: "neutral", name: "Avsluttet" };
    }
  }

  const { variant, name } = variantAndName();

  return (
    <Tag
      size="small"
      className="w-[140px] text-center whitespace-nowrap"
      aria-label={`Status for tiltakstype: ${name}`}
      variant={variant}
    >
      {name}
    </Tag>
  );
}
