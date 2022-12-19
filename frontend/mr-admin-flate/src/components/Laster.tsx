import { Loader } from "@navikt/ds-react";

interface Props {
  tekst?: string;
  sentrert?: boolean;
  size?:
    | "3xlarge"
    | "2xlarge"
    | "xlarge"
    | "large"
    | "medium"
    | "small"
    | "xsmall";
}

export function Laster({ tekst, sentrert = true, ...rest }: Props) {
  if (sentrert) {
    return (
      <div
        style={{
          display: "flex",
          justifyContent: "center",
          alignItems: "center",
          flexDirection: "column",
        }}
      >
        <Loader {...rest} />
        <p>{tekst}</p>
      </div>
    );
  }

  return (
    <div>
      <Loader {...rest} />
    </div>
  );
}
