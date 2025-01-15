import { ReactNode } from "react";

export function Bolk({ children, ...rest }: { children: ReactNode }) {
  return (
    <dl className="grid grid-cols-2 gap-12 my-4" {...rest}>
      {children}
    </dl>
  );
}
