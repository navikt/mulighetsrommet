import React from 'react';
import './Lenke.less';
import { Link as RouterLink, LinkProps as RouterLinkProps } from 'react-router-dom';
import classNames from 'classnames';
import { Link } from '@navikt/ds-react';

interface LinkProps extends RouterLinkProps {
  isInline?: boolean;
  isExternal?: boolean;
  children?: React.ReactNode;
}

function Lenke({ children, isExternal = false, isInline = false, to, className, ...others }: LinkProps) {
  return isExternal ? (
    <Link href={to.toString()} className={classNames('navds-link', className)} {...others}>
      {children}
    </Link>
  ) : (
    <RouterLink to={to} className={classNames('link', { lenke: isInline }, className)} {...others}>
      {children}
    </RouterLink>
  );
}

export default Lenke;
