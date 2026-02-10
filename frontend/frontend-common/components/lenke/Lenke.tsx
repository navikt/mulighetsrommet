import React, { MouseEventHandler } from "react";
import { Link as RouterLink, LinkProps as RouterLinkProps } from "react-router";
import { Link } from "@navikt/ds-react";
import { ExternalLinkIcon } from "@navikt/aksel-icons";

interface LinkProps extends RouterLinkProps {
  isExternal?: boolean;
  children?: React.ReactNode;
  onClick?: MouseEventHandler<HTMLAnchorElement>;
}

export function Lenke({ children, isExternal = false, to, className, onClick }: LinkProps) {
  return isExternal ? (
    <Link target="_blank" href={to.toString()} className={className} onClick={onClick}>
      {children}
      <ExternalLinkIcon aria-label="Ekstern lenke" />
    </Link>
  ) : (
    <RouterLink to={to} className={className} onClick={onClick}>
      {children}
    </RouterLink>
  );
}
