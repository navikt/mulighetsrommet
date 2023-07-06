import { ReactNode } from "react";

export function VisHvisVerdi({
  verdi,
  children,
}: {
  verdi?: any;
  children: ReactNode;
}) {
  return verdi ? children : null;
}
