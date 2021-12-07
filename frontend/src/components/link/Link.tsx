import React from 'react';
import cn from 'classnames';
import './Link.less';
import 'nav-frontend-lenker-style';
import Lenke from 'nav-frontend-lenker';
import { Link as RouterLink, LinkProps as RouterLinkProps } from 'react-router-dom';

interface LinkProps extends RouterLinkProps {
  isInline?: boolean;
  isExternal?: boolean;
  children?: React.ReactNode;
}

function Link({ children, isExternal = false, isInline = false, to, className, ...others }: LinkProps) {
  return isExternal ? (
    <Lenke href={to.toString()} className={className} {...others}>
      {children}
    </Lenke>
  ) : (
    <RouterLink to={to} className={cn('link', { lenke: isInline }, className)} {...others}>
      {children}
    </RouterLink>
  );
}

export default Link;
