import { BodyShort, Loader } from "@navikt/ds-react";

interface Props {
  tekst?: string;
  size?: "3xlarge" | "2xlarge" | "xlarge" | "large" | "medium" | "small" | "xsmall";
}

export function Laster({ tekst, ...rest }: Props) {
  if (tekst) {
    return (
      <div className="flex flex-col items-center justify-center my-8">
        <Loader {...rest} />
        <BodyShort>{tekst}</BodyShort>
      </div>
    );
  }

  return (
    <div>
      <Loader {...rest} />
    </div>
  );
}
