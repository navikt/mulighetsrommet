import { ReactNode } from "react";

export function VisHvisVerdi<T>({ verdi, children }: { verdi?: T; children: ReactNode }) {
  return verdi ? children : null;
}
