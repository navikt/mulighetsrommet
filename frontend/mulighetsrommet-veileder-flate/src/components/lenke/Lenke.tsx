import React from 'react';
import { Link as RouterLink, LinkProps as RouterLinkProps } from 'react-router-dom';
import classNames from 'classnames';
import { Link } from '@navikt/ds-react';
import { ExternalLink } from '@navikt/ds-icons';
import styles from './Lenke.module.scss';

interface LinkProps extends RouterLinkProps {
  isInline?: boolean;
  isExternal?: boolean;
  children?: React.ReactNode;
}

function Lenke({ children, isExternal = false, isInline = false, to, className, ...others }: LinkProps) {
  return isExternal ? (
    <Link target="_blank" href={to.toString()} className={classNames('navds-link', className)} {...others}>
      {children}
      <ExternalLink aria-label="Ekstern lenke" />
    </Link>
  ) : (
    <RouterLink to={to} className={classNames(styles.link, className)} {...others}>
      {children}
    </RouterLink>
  );
}

export default Lenke;
