import { PropsWithChildren } from "react";

export function PagineringContainer(props: PropsWithChildren) {
  return (
    <div className="flex justify-start py-4 pl-2 bg-[var(--a-gray-100)]">{props.children}</div>
  );
}
