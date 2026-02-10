import { ReactNode } from "react";
import { Button } from "@navikt/ds-react";
import { Link as ReactRouterLink } from "react-router";

interface Props {
  to: string;
  children: ReactNode;
  variant: "primary" | "secondary" | "tertiary";
  className?: string;
  size?: "small" | "medium";
  isExternal?: boolean;
  icon?: ReactNode;
  onClick?: () => void;
  id?: string;
}

export function Lenkeknapp({ to, children, icon, variant, size = "small" }: Props) {
  return (
    <Button as={ReactRouterLink} to={to} icon={icon} variant={variant} size={size}>
      {children}
    </Button>
  );
}
