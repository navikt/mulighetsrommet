import { TiltakstypeStatus } from "@tiltaksadministrasjon/api-client";
import { StatusTag } from "@mr/frontend-common";
import { getTiltakstypeStatusTagProps } from "./getStatusTagProps";

interface Props {
  status: TiltakstypeStatus;
}

export function TiltakstypeStatusTag({ status }: Props) {
  const { variant, name } = getTiltakstypeStatusTagProps(status);
  return (
    <StatusTag aria-label={`Status for tiltakstype: ${name}`} variant={variant}>
      {name}
    </StatusTag>
  );
}
