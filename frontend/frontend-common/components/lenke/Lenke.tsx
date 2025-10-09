import React, { MouseEventHandler } from "react";
import { Link as RouterLink, LinkProps as RouterLinkProps } from "react-router";
import { Link } from "@navikt/ds-react";
import { ExternalLinkIcon } from "@navikt/aksel-icons";

interface LinkProps extends RouterLinkProps {
  isExternal?: boolean;
  children?: React.ReactNode;
  onClick?: MouseEventHandler<HTMLAnchorElement>;
}

export function Lenke({ children, isExternal = false, to, ...others }: LinkProps) {
  return isExternal ? (
    <Link target="_blank" href={to.toString()} {...others}>
      {children}
      <ExternalLinkIcon aria-label="Ekstern lenke" />
    </Link>
  ) : (
    <Link as={RouterLink} to={to} {...others}>
      {children}
    </Link>
  );
}
