import { Link } from "@navikt/ds-react";
import { PropsWithChildren } from "react";

const PORTEN_URL = import.meta.env.PORTEN_URL ?? "";

export function PortenLink(props: PropsWithChildren) {
  const { children = "Porten" } = props;
  return (
    <Link href={PORTEN_URL} target={"_blank"}>
      {children}
    </Link>
  );
}
