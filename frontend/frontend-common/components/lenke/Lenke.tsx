import React, { MouseEventHandler } from "react";
import { Link as RouterLink, LinkProps as RouterLinkProps } from "react-router-dom";
import classNames from "classnames";
import { Link } from "@navikt/ds-react";
import styles from "./Lenke.module.scss";
import { ExternalLinkIcon } from "@navikt/aksel-icons";

interface LinkProps extends RouterLinkProps {
  isExternal?: boolean;
  children?: React.ReactNode;
  onClick?: MouseEventHandler<HTMLAnchorElement>;
}

export function Lenke({ children, isExternal = false, to, className, ...others }: LinkProps) {
  return isExternal ? (
    <Link
      target="_blank"
      href={to.toString()}
      className={classNames("navds-link", className)}
      {...others}
    >
      {children}
      <ExternalLinkIcon aria-label="Ekstern lenke" />
    </Link>
  ) : (
    <RouterLink to={to} className={classNames(styles.link, className)} {...others}>
      {children}
    </RouterLink>
  );
}
