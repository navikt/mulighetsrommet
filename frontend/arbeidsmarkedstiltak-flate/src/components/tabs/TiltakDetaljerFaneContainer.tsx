import { ReactNode } from "react";

interface Props {
  children: ReactNode;
  harInnhold: boolean;
  className?: string;
}

export function TiltakDetaljerFaneContainer({ children, harInnhold, className }: Props) {
  return <div className={className}>{harInnhold ? children : null}</div>;
}
