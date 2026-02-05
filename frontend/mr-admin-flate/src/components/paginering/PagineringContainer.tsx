import { PropsWithChildren } from "react";

export function PagineringContainer(props: PropsWithChildren) {
  return (
    <div className="flex justify-start py-4 pl-2 bg-[var(--ax-neutral-200)]">{props.children}</div>
  );
}
